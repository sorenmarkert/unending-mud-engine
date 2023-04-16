package core.commands

import core.ActRecipient.*
import core.ActVisibility.*
import core.MessageSender
import core.MiniMap.*
import core.commands.Commands
import core.gameunit.*
import core.gameunit.FindContext.*
import core.gameunit.GameUnit.findUnit
import core.storage.Storage
import core.util.MessagingUtils.*

import scala.util.*

class BasicCommands()(using storage: Storage, messageSender: MessageSender):

    import messageSender.*

    private[commands] def quit(character: Character, arg: Seq[String]) =
        character match
            case pc: PlayerCharacter =>
                sendMessage(pc, "Goodbye.")
                act("$1N has left the game.", Always, Some(character), None, None, ToAllExceptActor, None)
                pc.connection.close()
                storage.savePlayer(pc)
                pc.removeUnit()
            case _                   => // TODO: un-control NPC and quit controlling player

    private[commands] def look(character: Character, commandWords: Seq[String]) =

        commandWords match {
            case "look" :: Nil => character.outside foreach {
                case room: Room                    =>
                    val (items, chars) = room.contents filterNot (_.isInstanceOf[Room]) partition (_.isInstanceOf[Item])
                    val exits          = joinOrElse(room.exits.keys map (_.toString), ", ", "none")
                    val titles         = exits +: (items ++ chars filterNot (_ == character) map (unitDisplay(_)))
                    // TODO: adjust for character position
                    sendMessage(character, "%s\n   %s\nExits:  %s".format(
                        room.title,
                        room.description,
                        titles mkString "\n"), addMap = true)
                case item: Item                    =>
                    sendMessage(character, "\nYou are inside:\n%s\n%s\n%s".format(
                        item.title,
                        item.description,
                        item.contents map (unitDisplay(_)) mkString "\n"))
                case character: PlayerCharacter    =>
                    sendMessage(character, "\nYou are being carried by:\n%s\n%s\n%s".format(
                        character.name + " " + character.title,
                        character.description,
                        character.contents map (unitDisplay(_)) mkString "\n"))
                case character: NonPlayerCharacter =>
                    sendMessage(character, "\nYou are being carried by:\n%s\n%s\n%s".format(
                        character.title,
                        character.description,
                        character.contents map (unitDisplay(_)) mkString "\n"))
            }

            case "look" :: "in" :: Nil | "look" :: "inside" :: Nil => sendMessage(character, "Look inside what?")
            case "look" :: ("in" | "inside") :: argumentWords      =>
                findUnit(character, argumentWords mkString " ", Left(FindInOrNextToMe)) match {
                    case Some(unitToLookAt) =>
                        sendMessage(character, "You look inside the %s. It contains:\n%s".format(
                            unitToLookAt.name,
                            joinOrElse(unitToLookAt.contents map (unitDisplay(_)), "\n", "Nothing.")))
                    case None               => sendMessage(character, "No such thing here to look inside.")
                }

            case "look" :: "at" :: Nil             => sendMessage(character, "Look at what?")
            case "look" :: "at" :: _ | "look" :: _ =>
                val arg = if commandWords(1) == "at" then commandWords drop 2 else commandWords.tail

                findUnit(character, arg mkString " ", Left(FindInOrNextToMe)) match {
                    case Some(unitToLookAt: Character) => sendMessage(character, "You look at the %s.\n%s\n%s".format(
                        unitToLookAt.name,
                        unitToLookAt.description,
                        // TODO: display item slots
                        unitToLookAt.equippedItems map (_.title) mkString "\n"))
                    case Some(unitToLookAt)            => sendMessage(character, "You look at the %s.\n%s".format(
                        unitToLookAt.name,
                        unitToLookAt.description))
                    case None                          => sendMessage(character, "No such thing here to look at.")
                }
        }
    end look

    private[commands] def movement(character: Character, commandWords: Seq[String]) =
        // TODO: check if allowed to move
        val direction = Direction.valueOf(commandWords.head.toLowerCase.capitalize)
        character.outside match {
            case Some(room: Room) =>
                room.exits.get(direction) match {
                    case Some(Exit(toRoom, _)) =>
                        act("$1n leaves $1t.", Always, Some(character), None, None, ToAllExceptActor, Some(direction.display))
                        toRoom addUnit character
                        act("$1n has arrived from the $1t.", Always, Some(character), None, None, ToAllExceptActor, Some(direction.display))
                        look(character, "look" :: Nil)
                    case None                  => sendMessage(character, "No such exit here.")
                }
            case _                => sendMessage(character, "You can't do that in here.")
        }

    private[commands] def minimap(character: Character, commandWords: Seq[String]) =

        val range = Try(commandWords(1).toInt) match
            case Success(value) => value
            case Failure(_)     => 2

        character.outside match
            case Some(room: Room) => sendMessage(character, colourMiniMap(frameMiniMap(miniMap(room, range))) mkString "\n")
            case _                => sendMessage(character, "You can't see from in here.")

end BasicCommands
