package core.storage

import akka.event.slf4j.SLF4JLogging
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.Config
import core.gameunit.*
import net.ravendb.client.documents.DocumentStore
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.util.Base64
import scala.util.*

class RavenDBStorage(using config: Config) extends Storage with SLF4JLogging:

    private val ravenConfig = config.getConfig("storage.ravendb")
    private val certificate = ravenConfig.getString("certificate")
    private val password = ravenConfig.getString("password")
    private val hostname = ravenConfig.getString("hostname")
    private val database = ravenConfig.getString("database")

    private val documentStore = new DocumentStore
    documentStore.setCertificate(getKeyStore)
    documentStore.setDatabase(database)
    documentStore.setUrls(Array("https://" + hostname))
    documentStore.getConventions.getEntityMapper.registerModule(DefaultScalaModule)
    documentStore.initialize

    log.info(s"Connected to $hostname")

    override def savePlayer(playerCharacter: PlayerCharacter): Unit =

        def mapContainable(gameUnit: Containable[?]): Option[GameUnitDB] = gameUnit match
            case item@Item(name, title, description, _) => Some(ItemDB("Item",
                name,
                title,
                description,
                item.itemSlot,
                item.contents flatMap mapContainable))
            case npc@NonPlayerCharacter(name, title, description, _) => Some(NonPlayerCharacterDB("NonPlayerCharacter",
                name,
                title,
                description,
                npc.equippedItems flatMap mapContainable,
                npc.inventory flatMap mapContainable))
            case _ => None

        Using(documentStore.openSession) { session =>
            val playerToStore = PlayerCharacterDB("PlayerCharacter",
                playerCharacter.name,
                playerCharacter.title,
                playerCharacter.description,
                playerCharacter.equippedItems flatMap mapContainable,
                playerCharacter.inventory flatMap mapContainable)
            session.store(playerToStore, playerCharacter.name)
            session.saveChanges()
        } match
            case Success(_) => log.info(s"Saved ${playerCharacter.name}")
            case Failure(exception) => log.error(s"Failed saving ${playerCharacter.name}", exception)


    override def loadPlayer(name: String): PlayerCharacter = ???

    override def close() = documentStore.close()

    private def getKeyStore =
        val keyStore = KeyStore.getInstance("PKCS12", new BouncyCastleProvider())
        keyStore.load(ByteArrayInputStream(Base64.getDecoder.decode(certificate)), password.toCharArray)
        keyStore


sealed trait GameUnitDB

case class PlayerCharacterDB(unitType: String, name: String, title: String, description: String,
                             equippedItems: Seq[GameUnitDB], inventory: Seq[GameUnitDB]) extends GameUnitDB

case class NonPlayerCharacterDB(unitType: String, name: String, title: String, description: String,
                                equippedItems: Seq[GameUnitDB], inventory: Seq[GameUnitDB]) extends GameUnitDB

case class ItemDB(unitType: String, name: String, title: String, description: String,
                  itemSlot: Option[ItemSlot], contents: Seq[GameUnitDB]) extends GameUnitDB