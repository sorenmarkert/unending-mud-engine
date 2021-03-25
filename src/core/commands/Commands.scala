package core.commands

import core.GameUnit.findUnit
import core.{Character, Disconnecting, FindInOrNextToMe, Item, NonPlayerCharacter, PlayerCharacter, Room}
import play.api.Logger

sealed trait Command

case class InstantCommand(func: (Character, Seq[String]) => Unit) extends Command

case class DurationCommand(duration: Int,
                           beginFunc: (Character, Seq[String]) => Option[String],
                           endFunc: (Character, Seq[String]) => Unit) extends Command

object Commands {

    private val logger = Logger(this.getClass)

    private val commandMap = Map(
        "look" -> InstantCommand(look),
        "quit" -> InstantCommand(quit),
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
                    .map(commandString => (commandMap(commandString), arguments filterNot (_.isBlank)))
                    .getOrElse((unknownCommand, Nil))
            }
        }

        val (command, argument) = resolveCommand(input)
        command.func(character, argument)
    }

    def sendMessage(character: Character, message: String) =
        character match {
            case PlayerCharacter(_, writer) => writer println (message + "\n\n(12)fake-prompt(12)")
            case _ =>
        }

    private[this] def quit(character: Character, arg: Seq[String]) = {
        character match {
            case pc: PlayerCharacter => {
                sendMessage(character, "Goodbye.")
                pc.connectionState = Disconnecting
            }
            case _ =>
        }
    }

    private[this] def look(character: Character, argumentWords: Seq[String]) = {

        argumentWords match {

            case Nil => character.outside foreach {
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

            case "in" :: Nil | "inside" :: Nil => sendMessage(character, "Look inside what?")
            case ("in" | "inside") :: args => {
                findUnit(character, args mkString " ", Left(FindInOrNextToMe)) match {
                    case Some(unitToLookAt) => sendMessage(character, "You look inside the %s. It contains:\n%s".format(
                        unitToLookAt.name,
                        unitToLookAt.contents map (_.title) mkString "\n"))
                    case None => sendMessage(character, "No such thing here to look inside.")
                }
            }

            case "at" :: Nil => sendMessage(character, "Look at what?")
            case _ => {
                val arg = if (argumentWords.head == "at") argumentWords.tail else argumentWords
                findUnit(character, arg mkString " ", Left(FindInOrNextToMe)) match {
                    case Some(unitToLookAt) => sendMessage(character, "You look at the %s\n%s".format(
                        unitToLookAt.name,
                        unitToLookAt.description))
                    case None => sendMessage(character, "No such thing here to look at.")
                }
            }
        }
    }
}
