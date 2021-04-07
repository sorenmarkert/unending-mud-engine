package core.commands

import core.Character
import core.commands.Commands.{act, sendMessage}

object CombatCommands {

    private[commands] def prepareSlash(character: Character, arg: Seq[String]): Option[String] = {
        sendMessage(character, "You raise your weapon behind you, preparing...")
        act("$1n raises $1s weapon behind $1m, preparing...", Always, Some(character), None, None, ToAllExceptActor, None)
        None
    }

    private[commands] def doSlash(character: Character, arg: Seq[String]) = {
        sendMessage(character, "Your split thin air with a wide slash.")
        act("$1n splits thin air with a wide slash.", Always, Some(character), None, None, ToAllExceptActor, None)
    }
}
