package core.commands

import core.commands.BasicCommands.{look, movement, quit}
import core.{Character, PlayerCharacter}

sealed trait Command

case class InstantCommand(func: (Character, Seq[String]) => Unit) extends Command

case class DurationCommand(duration: Int,
                           beginFunc: (Character, Seq[String]) => Option[String],
                           endFunc: (Character, Seq[String]) => Unit) extends Command

object Commands {

    private val commandMap = Map(
        "quit" -> InstantCommand(quit),
        "look" -> InstantCommand(look),
        "north" -> InstantCommand(movement),
        "south" -> InstantCommand(movement),
        "east" -> InstantCommand(movement),
        "west" -> InstantCommand(movement),
        "up" -> InstantCommand(movement),
        "down" -> InstantCommand(movement),
    )

    private val emptyInput = InstantCommand((char, _) => sendMessage(char, ""))
    private val unknownCommand = InstantCommand((char, _) => sendMessage(char, "What's that?"))

    def executeCommand(character: Character, input: String) = {

        def resolveCommand(input: String) = {
            val list = input.split(" ").toList
            list match {
                case "" :: _ | Nil => (emptyInput, Nil)
                case commandPrefix :: arguments => commandMap
                    .keys
                    .find(_ startsWith commandPrefix)
                    .map(commandString => (commandMap(commandString), commandString :: arguments filterNot (_.isBlank)))
                    .getOrElse((unknownCommand, Nil))
            }
        }

        val (command, argument) = resolveCommand(input)
        command.func(character, argument)
    }

    def sendMessage(character: Character, message: String) = character match {
        case PlayerCharacter(_, writer) => writer println (message + "\n\n(12/20)fake-prompt(12/20)")
        case _ =>
    }
}
