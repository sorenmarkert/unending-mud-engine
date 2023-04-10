package core.storage

import core.gameunit.PlayerCharacter

trait Storage:

    def savePlayer(playerCharacter: PlayerCharacter): Unit

    def loadPlayer(name: String): PlayerCharacter
