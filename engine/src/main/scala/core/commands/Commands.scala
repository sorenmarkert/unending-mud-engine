package core.commands

import core.*
import core.MessageSender.*
import core.CommandExecution
import core.commands.Commands.{Command, InstantCommand, TimedCommand}
import core.gameunit.*

import scala.concurrent.duration.{DurationInt, FiniteDuration}


class Commands(using basicCommands: BasicCommands, combatCommands: CombatCommands, equipmentCommands: EquipmentCommands, messageSender: MessageSender):

    import basicCommands.*
    import combatCommands.*
    import equipmentCommands.*
    import messageSender.*

    private val emptyInput     = InstantCommand((char, _) => sendMessage(char, ""))
    private val unknownCommand = InstantCommand((char, _) => sendMessage(char, "What's that?"))

    private val commandList: Seq[(String, Command)] = Seq(
        "quit" -> InstantCommand(quit),

        "north" -> InstantCommand(movement, addMiniMap = true),
        "south" -> InstantCommand(movement, addMiniMap = true),
        "east" -> InstantCommand(movement, addMiniMap = true),
        "west" -> InstantCommand(movement, addMiniMap = true),
        "up" -> InstantCommand(movement, addMiniMap = true),
        "down" -> InstantCommand(movement, addMiniMap = true),

        "look" -> InstantCommand(look, canInterrupt = true, addMiniMap = true),
        "minimap" -> InstantCommand(minimap, canInterrupt = true),

        "inventory" -> InstantCommand(inventory, canInterrupt = true),
        "equipment" -> InstantCommand(equipment, canInterrupt = true),
        "examine" -> InstantCommand(examine, canInterrupt = true),
        "get" -> InstantCommand(get),
        "take" -> InstantCommand(get),
        "drop" -> InstantCommand(drop),
        "put" -> InstantCommand(put),
        "place" -> InstantCommand(put),
        "give" -> InstantCommand(give),
        "wear" -> InstantCommand(wear),
        "remove" -> InstantCommand(remove),

        "slash" -> TimedCommand(2.seconds, prepareSlash, doSlash),
        )

    def executeCommand(character: Mobile, input: String)(using globalState: GlobalState): String =

        val inputWords = (input split " ") filterNot (_.isBlank)

        val (command, commandWords) =
            inputWords.toList match
                case "" :: _ | Nil              => (emptyInput, Nil)
                case commandPrefix :: arguments =>
                    commandList
                        .find { case (k, _) => k startsWith commandPrefix.toLowerCase } // TODO: use a trie
                        .map { case (commandString, command) => (command, commandString :: arguments) }
                        .getOrElse((unknownCommand, Nil))

        globalState.actorSystem tell CommandExecution(command, character, commandWords)

        commandWords.headOption getOrElse ""

end Commands

object Commands:

    type CommandFunction = (Mobile, Seq[String]) => Set[PlayerCharacter]

    sealed trait Command

    case class InstantCommand(func: CommandFunction, canInterrupt: Boolean = false, addMiniMap: Boolean = false) extends Command

    case class TimedCommand(baseDuration: FiniteDuration, beginFunc: CommandFunction, endFunc: CommandFunction) extends Command
