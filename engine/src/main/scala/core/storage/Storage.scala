package core.storage

import core.connection.Connection
import core.gameunit.PlayerCharacter

import scala.concurrent.Future

trait Storage extends AutoCloseable:

    def savePlayer(playerCharacter: PlayerCharacter): Unit

    def loadPlayer(name: String, connection: Connection): Future[Option[PlayerCharacter]]

    def isNameAvailable(name: String): Future[Boolean]
