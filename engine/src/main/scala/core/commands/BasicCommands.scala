package core.commands

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
                val recipients = sendMessageToBystandersOf(character, "$1N has left the game.")
                pc.connection.enqueueMessage(Seq("Goodbye."))
                pc.connection.sendEnqueuedMessages(Seq(), Seq())
                pc.connection.close()
                storage.savePlayer(pc)
                pc.destroy
                recipients
            case _ => Seq() // TODO: un-control NPC and quit controlling player

    private[commands] def look(character: Mobile, commandWords: Seq[String]) =

        commandWords match
            case "look" :: Nil =>
                val room = character.outside
                val chars = room.mobiles filterNot (_ == character)
                val exits = joinOrElse(room.exits.keys map (_.toString), ", ", "none")
                val titles =
                    collapseDuplicates(room.contents map (unitDisplay(_))) ++
                        collapseDuplicates(chars map (unitDisplay(_)))
                // TODO: adjust for character position
                sendMessageToCharacter(
                    character,
                    s"""$$BrightWhite${room.title}$$Reset
                       |$$s3${room.description}
                       |$$BrightYellowExits: $exits$$Reset
                       |${titles mkString "\n"}""".stripMargin)

            case "look" :: ("in" | "inside") :: Nil => sendMessageToCharacter(character, "Look inside what?")
            case "look" :: ("in" | "inside") :: argumentWords =>
                character.findItemInOrNextToMe(argumentWords mkString " ") match {
                    case None => sendMessageToCharacter(character, "No such thing here to look inside.")
                    case Some(unitToLookAt) =>
                        val itemsDisplay = joinOrElse(collapseDuplicates(unitToLookAt.contents map (unitDisplay(_))), "\n", "Nothing.")
                        sendMessageToCharacter(character,
                            s"""You look inside the ${unitToLookAt.name}. It contains:
                               |$itemsDisplay""".stripMargin)
                }

            case "look" :: "at" :: Nil => sendMessageToCharacter(character, "Look at what?")
            case "look" :: "at" :: _ | "look" :: _ =>
                val arg = if commandWords(1) == "at" then commandWords drop 2 else commandWords.tail

                character.findInOrNextToMe(arg mkString " ") match
                    case Some(unitToLookAt: Mobile) =>
                        val equipmentDisplay = getEquipmentDisplay(unitToLookAt) mkString "\n"
                        sendMessageToCharacter(
                            character,
                            s"""You look at the ${unitToLookAt.name}.
                               |${unitToLookAt.description}
                               |$equipmentDisplay""".stripMargin)
                    case Some(unitToLookAt) =>
                        sendMessageToCharacter(
                            character,
                            s"""You look at the ${unitToLookAt.name}.
                               |${unitToLookAt.description}""".stripMargin)
                    case None => sendMessageToCharacter(character, "No such thing here to look at.")
    end look

    private[commands] def movement(character: Mobile, commandWords: Seq[String]) =
        // TODO: check if allowed to move
        val direction = Direction.valueOf(commandWords.head.toLowerCase.capitalize)

        character.outside.exits.get(direction) match
            case Some(Exit(toRoom, _)) =>
                val recipientsFrom = sendMessageToBystandersOf(character, s"${character.name} leaves ${direction.display}.")
                toRoom.addMobile(character)
                val recipientsTo = sendMessageToBystandersOf(character, s"${character.name} has arrived from the ${direction.opposite.display}.")
                val recipientsChar = look(character, "look" :: Nil)
                recipientsFrom ++ recipientsTo ++ recipientsChar
            case None => sendMessageToCharacter(character, "No such exit here.")

    private[commands] def minimap(character: Mobile, commandWords: Seq[String]) =
        val range = Try(commandWords(1).toInt) match
            case Success(value) => value
            case Failure(_) => 2
        sendMessageToCharacter(character, colourMiniMap(frameMiniMap(miniMap(character.outside, range))) mkString "\n")

end BasicCommands
