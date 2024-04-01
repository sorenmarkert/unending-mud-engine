package core.commands

import core.ActRecipient.*
import core.ActVisibility.*
import core.MessageSender
import core.MiniMap.*
import core.gameunit.*
import core.storage.Storage
import core.util.MessagingUtils.*

import scala.util.*

class BasicCommands()(using storage: Storage, messageSender: MessageSender):

    import messageSender.*

    private[commands] def quit(character: Mobile, arg: Seq[String]) =
        character match
            case pc: PlayerCharacter =>
                sendMessage(pc, "Goodbye.", addPrompt = false)
                act("$1N has left the game.", Always, Some(character), None, None, ToAllExceptActor, None)
                pc.connection.close()
                storage.savePlayer(pc)
                pc.destroy
                Set(pc)
            case _                   => Set() // TODO: un-control NPC and quit controlling player

    private[commands] def look(character: Mobile, commandWords: Seq[String]) =

        commandWords match
            case "look" :: Nil =>
                val room   = character.outside
                val chars  = room.mobiles filterNot (_ == character)
                val exits  = joinOrElse(room.exits.keys map (_.toString), ", ", "none")
                val titles =
                    collapseDuplicates(room.contents map (unitDisplay(_))) ++
                    collapseDuplicates(chars map (unitDisplay(_)))
                // TODO: adjust for character position
                sendMessage(
                    character,
                    "$BrightWhite%s$Reset\n   %s\n$BrightYellowExits: %s$Reset\n%s".format(
                        room.title, room.description, exits, titles mkString "\n"),
                    addMiniMap = true)

            case "look" :: ("in" | "inside") :: Nil           => sendMessage(character, "Look inside what?")
            case "look" :: ("in" | "inside") :: argumentWords =>
                character.findItemInOrNextToMe(argumentWords mkString " ") match {
                    case None               => sendMessage(character, "No such thing here to look inside.")
                    case Some(unitToLookAt) =>
                        sendMessage(character, "You look inside the %s. It contains:\n%s".format(
                            unitToLookAt.name,
                            joinOrElse(collapseDuplicates(unitToLookAt.contents map (unitDisplay(_))), "\n", "Nothing.")))
                }

            case "look" :: "at" :: Nil             => sendMessage(character, "Look at what?")
            case "look" :: "at" :: _ | "look" :: _ =>
                val arg = if commandWords(1) == "at" then commandWords drop 2 else commandWords.tail

                character.findInOrNextToMe(arg mkString " ") match
                    case Some(unitToLookAt: Mobile) => sendMessage(character, "You look at the %s.\n%s\n%s".format(
                        unitToLookAt.name,
                        unitToLookAt.description,
                        // TODO: display item slots
                        unitToLookAt.equippedItems map (_.title) mkString "\n"))
                    case Some(unitToLookAt)         => sendMessage(character, "You look at the %s.\n%s".format(
                        unitToLookAt.name,
                        unitToLookAt.description))
                    case None                       => sendMessage(character, "No such thing here to look at.")
    end look

    private[commands] def movement(character: Mobile, commandWords: Seq[String]) =
        // TODO: check if allowed to move
        val direction = Direction.valueOf(commandWords.head.toLowerCase.capitalize)

        character.outside.exits.get(direction) match
            case Some(Exit(toRoom, _)) =>
                act("$1n leaves $1t.", Always, Some(character), None, None, ToAllExceptActor, Some(direction.display))
                toRoom addMobile character
                act("$1n has arrived from the $1t.", Always, Some(character), None, None, ToAllExceptActor, Some(direction.display))
                look(character, "look" :: Nil)
            case None                  => sendMessage(character, "No such exit here.")

    private[commands] def minimap(character: Mobile, commandWords: Seq[String]) =
        val range = Try(commandWords(1).toInt) match
            case Success(value) => value
            case Failure(_)     => 2
        sendMessage(character, colourMiniMap(frameMiniMap(miniMap(character.outside, range))) mkString "\n")

end BasicCommands
