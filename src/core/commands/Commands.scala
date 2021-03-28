package core.commands

import core.commands.BasicCommands.{look, movement, quit}
import core.commands.EquipmentCommands.{drop, equipment, get, inventory, put}
import core.{Character, PlayerCharacter}

sealed trait Command

case class InstantCommand(func: (Character, Seq[String]) => Unit) extends Command

case class DurationCommand(duration: Int,
                           beginFunc: (Character, Seq[String]) => Option[String],
                           endFunc: (Character, Seq[String]) => Unit) extends Command

object Commands {

    private val commandMap = Map(
        "quit" -> InstantCommand(quit),
        "north" -> InstantCommand(movement),
        "south" -> InstantCommand(movement),
        "east" -> InstantCommand(movement),
        "west" -> InstantCommand(movement),
        "up" -> InstantCommand(movement),
        "down" -> InstantCommand(movement),
        "look" -> InstantCommand(look),
        "inventory" -> InstantCommand(inventory),
        "equipment" -> InstantCommand(equipment),
        "get" -> InstantCommand(get),
        "take" -> InstantCommand(get),
        "drop" -> InstantCommand(drop),
        "put" -> InstantCommand(put),
        "place" -> InstantCommand(put),
    )

    private val emptyInput = InstantCommand((char, _) => sendMessage(char, ""))
    private val unknownCommand = InstantCommand((char, _) => sendMessage(char, "What's that?"))

    def executeCommand(character: Character, input: String) = {

        val inputWords = input.split(" ").toList

        val (command, argument) = inputWords match {
            case "" :: _ | Nil => (emptyInput, Nil)
            case commandPrefix :: arguments => commandMap
                .keys
                .find(_ startsWith commandPrefix) // TODO: use a trie
                .map(commandString => (commandMap(commandString), commandString :: arguments filterNot (_.isBlank)))
                .getOrElse((unknownCommand, Nil))
        }

        command.func(character, argument)
    }

    def sendMessage(character: Character, message: String) = character match {
        case PlayerCharacter(_, writer) => writer println (message + "\n\n(12/20)fake-prompt(12/20)")
        case _ =>
    }

    private[commands] def joinOrElse(strings: Iterable[String], separator: String, default: String) = {
        Option(strings)
            .filterNot(_.isEmpty)
            .map(_ mkString separator)
            .getOrElse(default)
    }
}
