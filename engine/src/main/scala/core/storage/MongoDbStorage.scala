package core.storage

import akka.event.slf4j.{Logger, SLF4JLogging}
import com.mongodb.client.model.{ReplaceOptions, UpdateOptions}
import com.typesafe.config.Config
import core.gameunit.*
import org.mongodb.scala.bson.{BsonArray, BsonElement, BsonValue, Document}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.result.{InsertOneResult, UpdateResult}
import org.mongodb.scala.{MongoClient, MongoDatabase, Observer, Subscription}

class MongoDbStorage(using config: Config) extends Storage with SLF4JLogging:


    private val mongoConfig = config.getConfig("storage.mongodb")
    private val username    = mongoConfig.getString("username")
    private val password    = mongoConfig.getString("password")
    private val hostname    = mongoConfig.getString("hostname")
    private val database    = mongoConfig.getString("database")

    private val client  = MongoClient(s"mongodb+srv://$username:$password@$hostname/?retryWrites=true&w=majority")
    private val db      = client.getDatabase(database)
    private val players = db.getCollection("players")

    log.info(s"Connected to $hostname")

    override def savePlayer(playerCharacter: PlayerCharacter): Unit =

        def mapCommonAttributes(gameUnit: Containable[_]) =
            Document("unitType" -> gameUnit.getClass.getSimpleName,
                     "name" -> gameUnit.name,
                     "title" -> gameUnit.title,
                     "description" -> gameUnit.description)

        def mapGameUnit(gameUnit: Containable[_]): Document = gameUnit match
            case item: Item              =>
                mapCommonAttributes(gameUnit)
                    ++ Document("itemSlot" -> (item.itemSlot map (_.toString) getOrElse ""))
                    ++ Document(Map("contents" -> (item.contents map mapGameUnit)))
            case npc: NonPlayerCharacter => mapCharacter(npc)
            case _                       => Document()

        def mapCharacter(character: Mobile): Document =
            mapCommonAttributes(character)
                ++ Document(Map("inventory" -> (character.inventory map mapGameUnit)))
                ++ Document(Map("equippedItems" -> (playerCharacter.equippedItems map mapGameUnit)))

        val document = mapCharacter(playerCharacter)

        players
            .replaceOne(
                equal("_id", playerCharacter.name),
                document,
                ReplaceOptions().upsert(true))
            .subscribe(new Observer[UpdateResult] {
                override def onNext(result: UpdateResult): Unit = {}

                override def onError(e: Throwable): Unit = log.error(s"Failed saving ${playerCharacter.name}", e)

                override def onComplete(): Unit = log.info(s"Saved ${playerCharacter.name}")
            })


    override def loadPlayer(name: String): PlayerCharacter = ???

    override def close() = client.close()
