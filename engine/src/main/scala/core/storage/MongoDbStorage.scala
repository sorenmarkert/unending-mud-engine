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

    private val username = config.getString("storage.mongodb.username")
    private val password = config.getString("storage.mongodb.password")
    private val hostname = config.getString("storage.mongodb.hostname")
    private val database = config.getString("storage.mongodb.database")

    private val uri     = s"mongodb+srv://$username:$password@$hostname/?retryWrites=true&w=majority"
    private val client  = MongoClient(uri)
    private val db      = client.getDatabase(database)
    private val players = db.getCollection("players")

    override def savePlayer(playerCharacter: PlayerCharacter): Unit =

        def mapCommonAttributes(gameUnit: GameUnit) = Document("type" -> gameUnit.getClass.getSimpleName,
                                                               "name" -> gameUnit.name,
                                                               "title" -> gameUnit.title,
                                                               "description" -> gameUnit.description)

        def mapGameUnit(gameUnit: GameUnit): Document = gameUnit match
            case item@Item(_)              =>
                mapCommonAttributes(gameUnit)
                    ++ Document("itemSlot" -> item.itemSlot.map(_.ordinal.toString).getOrElse(""))
                    ++ Document(Map("contents" -> (item.contents map mapGameUnit)))
            case npc@NonPlayerCharacter(_) => mapCharacter(npc)
            case PlayerCharacter(_, _)     => Document()
            case Room(_)                   => Document()

        def mapCharacter(character: Character): Document =
            mapCommonAttributes(character)
                ++ Document(Map("inventory" -> (character.inventory map mapGameUnit)))
                ++ Document(Map("equippedItems" -> (playerCharacter.equippedItems map mapGameUnit)))

        val document = mapCharacter(playerCharacter)

        players
            .replaceOne(
                equal("_id", playerCharacter.__id),
                document,
                ReplaceOptions().upsert(true))
            .subscribe(new Observer[UpdateResult] {
                override def onNext(result: UpdateResult): Unit = {}

                override def onError(e: Throwable): Unit = log.error(s"Failed saving ${playerCharacter.name}", e)

                override def onComplete(): Unit = log.info(s"Saved ${playerCharacter.name}")
            })


    override def loadPlayer(name: String): PlayerCharacter = ???

    override def close() = client.close()
