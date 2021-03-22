package core.commands

import core.{Character, Disconnecting, Item, NonPlayerCharacter, PlayerCharacter, Room}

sealed trait Command

case class InstantCommand(func: (Character, String) => Unit) extends Command

case class DurationCommand(duration: Int, beginFunc: (Character, String) => Unit, endFunc: (Character, String) => Unit) extends Command

object Commands {

    private val commandMap = Map(
        "look" -> InstantCommand(look),
        "logout" -> InstantCommand(logout),
    )

    private val emptyInput = InstantCommand((char, _) => sendMessage(char, ""))
    private val unknownCommand = InstantCommand((char, _) => sendMessage(char, "What's that?"))

    def executeCommand(character: Character, input: String) = {

        def resolveCommand(input: String) = {
            val list = input.split(" ").toList
            list match {
                case "" :: _ | Nil => (emptyInput, "")
                case commandPrefix :: arguments => commandMap
                    .keys
                    .find(_ startsWith commandPrefix)
                    .map(command => (commandMap(command), arguments filterNot (_.isBlank) mkString " "))
                    .getOrElse((unknownCommand, ""))
            }
        }

        val (command, argument) = resolveCommand(input)
        command.func(character, argument)
    }

    def sendMessage(character: Character, message: String) =
        character match {
            case PlayerCharacter(_, writer) => writer println (message + "\n(12)fake-prompt(12)")
            case _ =>
        }

    private[this] def logout(character: Character, arg: String) = {
        character match {
            case pc: PlayerCharacter => {
                sendMessage(character, "Goodbye.")
                pc.connectionState = Disconnecting
            }
            case _ =>
        }
    }

    private[this] def look(character: Character, arg: String) = {
        character.outside foreach {
            case room: Room => {
                val (items, chars) = room.contents filterNot (_.isInstanceOf[Room]) partition (_.isInstanceOf[Item])
                sendMessage(character, "%s\n   %s\nExits: %s\n%s\n%s".format(
                    room.title,
                    room.description,
                    room.exits.keys.mkString(", "),
                    items map (_.title) mkString "\n",
                    chars filterNot (_ == character) map (_.title) mkString "\n"))
            }
            case item: Item =>
                sendMessage(character, "\nYou are inside:\n%s\n%s\n%s".format(
                    item.title,
                    item.description,
                    item.contents map (_.title) mkString "\n"))
            case character: PlayerCharacter =>
                sendMessage(character, "\nYou are being carried by:\n%s\n%s\n%s".format(
                    character.name + " " + character.title,
                    character.description,
                    character.contents map (_.title) mkString "\n"))
            case character: NonPlayerCharacter =>
                sendMessage(character, "\nYou are being carried by:\n%s\n%s\n%s".format(
                    character.title,
                    character.description,
                    character.contents map (_.title) mkString "\n"))
            case _ =>
        }
    }
}
