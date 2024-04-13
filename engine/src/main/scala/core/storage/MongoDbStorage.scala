package core.storage

import akka.event.slf4j.SLF4JLogging
import com.mongodb.client.model.ReplaceOptions
import com.typesafe.config.Config
import core.commands.Commands
import core.connection.Connection
import core.gameunit.*
import core.state.GlobalState
import org.mongodb.scala.*
import org.mongodb.scala.bson.Document
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Aggregates.*
import org.mongodb.scala.model.Filters.equal

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.Try

class MongoDbStorage()(using config: Config, globalState: GlobalState, commands: Commands) extends Storage with SLF4JLogging:

    private val mongoConfig = config.getConfig("storage.mongodb")
    private val username = mongoConfig.getString("username")
    private val password = mongoConfig.getString("password")
    private val hostname = mongoConfig.getString("hostname")
    private val database = mongoConfig.getString("database")

    private val client = MongoClient(s"mongodb+srv://$username:$password@$hostname/?retryWrites=true&w=majority")
    private val db = client.getDatabase(database)
    private val players = db.getCollection("players")

    log.info(s"Connected to $hostname")

    override def savePlayer(playerCharacter: PlayerCharacter): Unit =

        def mapCommonAttributes(gameUnit: Containable[?]) =
            Document(
                "name" -> gameUnit.name,
                "title" -> gameUnit.title,
                "description" -> gameUnit.description)

        def mapItem(item: Item): Document =
            mapCommonAttributes(item) ++ Document(
                "isEquipped" -> item.isEquipped,
                "itemSlot" -> (item.itemSlot map (_.toString) getOrElse ""),
                "contents" -> (item.contents map mapItem))

        val playerAsDocument = mapCommonAttributes(playerCharacter) ++ Document(
            "room" -> playerCharacter.outside.id,
            "contents" -> (playerCharacter.contents map mapItem))

        players
            .replaceOne(
                equal("_id", playerCharacter.name),
                playerAsDocument,
                ReplaceOptions().upsert(true))
            .subscribe(
                result => log.debug(s"UpdateResult: $result"),
                e => log.error(s"Failed saving player ${playerCharacter.name}", e),
                () => log.info(s"Saved player ${playerCharacter.name}"))


    override def loadPlayer(name: String, connection: Connection): Future[Option[PlayerCharacter]] =

        val capitalizedName = name.toLowerCase.capitalize
        def mapContents(document: Document, gameUnit: GameUnit): Unit =
            document.getOrElse("contents", Seq.empty[Document]).asArray().getValues.asScala.reverse
                .foreach { v => mapItem(v.asDocument(), gameUnit) }

        def mapItem(document: Document, gameUnit: GameUnit): Unit =
            val item = gameUnit.createItem(
                document.getOrElse("name", "").asString().getValue,
                document.getOrElse("title", "").asString().getValue,
                document.getOrElse("description", "").asString().getValue)
            mapContents(document, item)
            item.itemSlot = Try(ItemSlot.valueOf(document.getOrElse("itemSlot", "").asString().getValue)).toOption
            (gameUnit, document.getOrElse("isEquipped", "false").asBoolean().getValue) match
                case (character: Mobile, true) => character.equip(item)
                case _ =>

        def mapToPlayer(document: Document): PlayerCharacter =
            val startingRoom = globalState.rooms(document.getOrElse("room", "roomCenter").asString().getValue)
            val playerCharacter = startingRoom.createPlayerCharacter(capitalizedName, connection)
            playerCharacter.name = document.getOrElse("name", "").asString().getValue
            playerCharacter.title = document.getOrElse("title", "").asString().getValue
            playerCharacter.description = document.getOrElse("description", "").asString().getValue
            mapContents(document, playerCharacter)
            playerCharacter

        players
            .find(equal("_id", capitalizedName))
            .first
            .map(mapToPlayer)
            .headOption()

    override def isNameAvailable(name: String): Future[Boolean] =
        players
            .countDocuments(
                equal("_id", name.toLowerCase.capitalize),
                CountOptions().limit(1))
            .map(_ < 1L)
            .headOption()
            .map(_ getOrElse false)

    override def close(): Unit = client.close()
