package core.storage

import core.connection.Connection
import core.gameunit.PlayerCharacter

trait Storage extends AutoCloseable:

    def checkName(name: String): Boolean

    def savePlayer(playerCharacter: PlayerCharacter): Unit

    def loadPlayer(name: String, connection: Connection, connectPlayer: PlayerCharacter => Unit): Unit
