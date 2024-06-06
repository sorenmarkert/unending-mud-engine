package core.commands

import akka.actor.typed.ActorSystem
import core.*
import core.MessageSender.*
import core.commands.Commands.{Command, CommandResult, InstantCommand, TimedCommand}
import core.gameunit.*
import core.state.StateActor
import core.state.StateActor.Execute

import scala.collection.SeqMap
import scala.concurrent.duration.{DurationInt, FiniteDuration}


class Commands(using actorSystem: ActorSystem[StateActor.Message], basicCommands: BasicCommands, combatCommands: CombatCommands, equipmentCommands: EquipmentCommands, utilityCommands: UtilityCommands, messageSender: MessageSender):

    import basicCommands.*
    import combatCommands.*
    import equipmentCommands.*
    import utilityCommands.*
    import messageSender.*

    private val emptyInput = InstantCommand((char, _) => CommandResult(sendMessageToCharacter(char, "")))
    private val unknownCommand = InstantCommand((char, _) => CommandResult(sendMessageToCharacter(char, "What's that?")))

    private[commands] val commandsByWord: SeqMap[String, Command] = SeqMap(
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
        "path" -> InstantCommand(path),

        "slash" -> TimedCommand(2.seconds, prepareSlash, doSlash),
    )

    def executeCommandAtNextTick(character: Mobile, input: String): Unit =

        val inputWords = input.split(" ") filterNot (_.isBlank)

        val (command, commandWords) =
            inputWords.toList match
                case Nil => (emptyInput, Nil)
                case receivedCommandPrefix :: arguments =>
                    commandsByWord
                        .find { case (commandWord, _) => commandWord.startsWith(receivedCommandPrefix.toLowerCase) }
                        .map { case (commandWord, command) => (command, commandWord :: arguments) }
                        .getOrElse((unknownCommand, Nil))

        actorSystem tell Execute(command, character, commandWords)

end Commands


object Commands:

    case class CommandResult(playersWhoReceivedMessages: Seq[PlayerCharacter], addMiniMap: Boolean = false)

    private type CommandFunction = (Mobile, Seq[String]) => CommandResult

    sealed trait Command

    case class InstantCommand(func: CommandFunction, canInterrupt: Boolean = false) extends Command

    case class TimedCommand(baseDuration: FiniteDuration, beginFunc: CommandFunction, endFunc: CommandFunction) extends Command
