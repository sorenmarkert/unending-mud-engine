package core.commands

import akka.actor.typed.ActorSystem
import core.*
import core.MessageSender.*
import core.commands.Commands.{Command, CommandResult, InstantCommand, TimedCommand}
import core.gameunit.*
import core.state.{CommandExecution, StateActorMessage}

import scala.concurrent.duration.{DurationInt, FiniteDuration}


class Commands(using actorSystem: ActorSystem[StateActorMessage], basicCommands: BasicCommands, combatCommands: CombatCommands, equipmentCommands: EquipmentCommands, messageSender: MessageSender):

    import basicCommands.*
    import combatCommands.*
    import equipmentCommands.*
    import messageSender.*

    private val emptyInput = InstantCommand((char, _) => CommandResult(sendMessageToCharacter(char, "")))
    private val unknownCommand = InstantCommand((char, _) => CommandResult(sendMessageToCharacter(char, "What's that?")))

    private val commandList: Seq[(String, Command)] = Seq(
        "quit" -> InstantCommand(quit),

        "north" -> InstantCommand(movement),
        "south" -> InstantCommand(movement),
        "east" -> InstantCommand(movement),
        "west" -> InstantCommand(movement),
        "up" -> InstantCommand(movement),
        "down" -> InstantCommand(movement),

        "look" -> InstantCommand(look, canInterrupt = true),
        "minimap" -> InstantCommand(minimap, canInterrupt = true),

        "inventory" -> InstantCommand(inventory, canInterrupt = true),
        "equipment" -> InstantCommand(equipment, canInterrupt = true),
        "examine" -> InstantCommand(examine, canInterrupt = true),
        "get" -> InstantCommand(get),
        "drop" -> InstantCommand(drop),
        "put" -> InstantCommand(put),
        "give" -> InstantCommand(give),
        "wear" -> InstantCommand(wear),
        "remove" -> InstantCommand(remove),

        "slash" -> TimedCommand(2.seconds, prepareSlash, doSlash),
    )

    def executeCommandAtNextTick(character: Mobile, input: String): Unit =

        val inputWords = input.split(" ") filterNot (_.isBlank)

        val (command, commandWords) =
            inputWords.toList match
                case "" :: _ | Nil => (emptyInput, Nil)
                case receivedCommandPrefix :: arguments =>
                    commandList
                        .find { case (commandString, _) => commandString.startsWith(receivedCommandPrefix.toLowerCase) }
                        .map { case (commandString, command) => (command, commandString :: arguments) }
                        .getOrElse((unknownCommand, Nil))

        actorSystem tell CommandExecution(command, character, commandWords)

end Commands


object Commands:

    case class CommandResult(playersWhoReceivedMessages: Seq[PlayerCharacter], addMiniMap: Boolean = false)

    type CommandFunction = (Mobile, Seq[String]) => CommandResult

    sealed trait Command

    case class InstantCommand(func: CommandFunction, canInterrupt: Boolean = false) extends Command

    case class TimedCommand(baseDuration: FiniteDuration, beginFunc: CommandFunction, endFunc: CommandFunction) extends Command
