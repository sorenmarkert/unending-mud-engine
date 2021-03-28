package core.commands

import core.GameUnit.findUnit
import core.commands.Commands.{act, joinOrElse, sendMessage}
import core.{Character, Direction, Disconnecting, FindInOrNextToMe, Item, NonPlayerCharacter, PlayerCharacter, Room}

object BasicCommands {

    private[commands] def quit(character: Character, arg: Seq[String]) = {
        character match {
            case pc: PlayerCharacter => {
                sendMessage(character, "Goodbye.")
                act("$1N has left the game.", Always, Some(character), None, None, ToAllExceptActor, None)
                pc.connectionState = Disconnecting
            }
            case _ =>
        }
    }

    private[commands] def look(character: Character, commandWords: Seq[String]) = {

        commandWords match {
            case "look" :: Nil => character.outside foreach {
                case room: Room => {
                    val (items, chars) = room.contents filterNot (_.isInstanceOf[Room]) partition (_.isInstanceOf[Item])
                    val exits = joinOrElse(room.exits.keys map (_.toString), ", ", "none")
                    val titles = exits +: (items ++ chars filterNot (_ == character) map (_.title)) // TODO: adjust for position
                    sendMessage(character, "%s\n   %s\nExits:  %s".format(
                        room.title,
                        room.description,
                        titles mkString "\n"))
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

            case "look" :: "in" :: Nil | "look" :: "inside" :: Nil => sendMessage(character, "Look inside what?")
            case "look" :: ("in" | "inside") :: argumentWords => {
                findUnit(character, argumentWords mkString " ", Left(FindInOrNextToMe)) match {
                    case Some(unitToLookAt) =>
                        sendMessage(character, "You look inside the %s. It contains:\n%s".format(
                            unitToLookAt.name,
                            joinOrElse(unitToLookAt.contents map (_.title), "\n", "Nothing.")))
                    case None => sendMessage(character, "No such thing here to look inside.")
                }
            }

            case "look" :: "at" :: Nil => sendMessage(character, "Look at what?")
            case "look" :: "at" :: _ | "look" :: _ => {
                val arg = if (commandWords(1) == "at") commandWords drop 2 else commandWords.tail
                findUnit(character, arg mkString " ", Left(FindInOrNextToMe)) match {
                    case Some(unitToLookAt) => sendMessage(character, "You look at the %s\n%s".format(
                        unitToLookAt.name,
                        unitToLookAt.description))
                    case None => sendMessage(character, "No such thing here to look at.")
                }
            }
        }
    }

    private[commands] def movement(character: Character, commandWords: Seq[String]) = {
        // TODO: check if allowed to move
        val direction = Direction.values find (_.toString == commandWords.head)
        character.outside match {
            case Some(room: Room) => {
                room.exits.get(direction.get) match {
                    case Some(toRoom) => {
                        act("$1n leaves $1t.", Always, Some(character), None, None, ToAllExceptActor, Some(direction.get.toString))
                        toRoom addUnit character
                        act("$1n has arrived from the $1t.", Always, Some(character), None, None, ToAllExceptActor, Some(direction.get.toString))
                        look(character, "look" :: Nil)
                    }
                    case None => sendMessage(character, "No such exit here.")
                }
            }
            case _ => sendMessage(character, "You can't do that in here.")
        }
    }
}
