package core.commands

import core.*
import core.commands.Commands.CommandResult
import core.gameunit.{Mobile, PlayerCharacter}

class CombatCommands(using messageSender: MessageSender):

    import messageSender.*

    private[commands] def prepareSlash(character: Mobile, arg: Seq[String]): CommandResult =
        CommandResult(act("$1n $[raise|raises] $1s weapon behind $1m, preparing...", ActVisibility.Always, Some(character)))

    private[commands] def doSlash(character: Mobile, arg: Seq[String]): CommandResult =
        CommandResult(act("$1n $[split|splits] thin air with a wide slash.", ActVisibility.Always, Some(character)))
