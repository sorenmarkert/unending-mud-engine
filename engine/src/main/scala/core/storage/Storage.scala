package core.storage

import core.gameunit.PlayerCharacter

trait Storage extends AutoCloseable:

    def savePlayer(playerCharacter: PlayerCharacter): Unit

    def loadPlayer(name: String): PlayerCharacter
