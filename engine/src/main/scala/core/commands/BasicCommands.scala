package core.commands

import core.MessageSender
import core.MiniMap.*
import core.commands.Commands.CommandResult
import core.gameunit.*
import core.storage.Storage
import core.util.MessagingUtils.*

import scala.util.*

class BasicCommands()(using storage: Storage, messageSender: MessageSender):

    import messageSender.*

    private[commands] def quit(character: Mobile, arg: Seq[String]): CommandResult =
        val recipients = character match
            case pc: PlayerCharacter =>
                val recipients = sendMessageToBystandersOf(character, "$1N has left the game.")
                pc.connection.enqueueMessage(Seq("Goodbye."))
                pc.connection.sendEnqueuedMessages(Seq(), Seq())
                pc.connection.close()
                storage.savePlayer(pc)
                pc.destroy
                recipients
            case _ => Seq()
        CommandResult(recipients)

    private[commands] def movement(character: Mobile, commandWords: Seq[String]): CommandResult =
        val direction = Direction.valueOf(commandWords.head.toLowerCase.capitalize)

        character.outside.exits.get(direction) match
            case None => CommandResult(sendMessageToCharacter(character, "No such exit here."))
            case Some(Exit(toRoom, _)) =>
                val recipientsFrom = sendMessageToBystandersOf(character, s"${character.name} leaves ${direction.display}.")
                toRoom.addMobile(character)
                val recipientsTo = sendMessageToBystandersOf(character, s"${character.name} has arrived from the ${direction.opposite.display}.")
                val recipientsChar = look(character, "look" :: Nil).playersWhoReceivedMessages
                CommandResult(recipientsFrom ++ recipientsTo ++ recipientsChar, addMiniMap = true)

    private[commands] def minimap(character: Mobile, commandWords: Seq[String]): CommandResult =
        val range = Try(commandWords(1).toInt) match
            case Success(value) => value
            case Failure(_) => 2
        CommandResult(sendMessageToCharacter(character, colourMiniMap(frameMiniMap(miniMap(character.outside, range))) mkString "\n"))

    private[commands] def look(character: Mobile, commandWords: Seq[String]): CommandResult =
        commandWords match
            case "look" :: Nil => lookAtRoom(character)
            case "look" :: ("in" | "inside") :: Nil => CommandResult(sendMessageToCharacter(character, "Look inside what?"))
            case "look" :: ("in" | "inside") :: argumentWords => lookInside(character, argumentWords)
            case "look" :: "at" :: Nil => CommandResult(sendMessageToCharacter(character, "Look at what?"))
            case "look" :: "at" :: argumentWords => lookAt(character, argumentWords)
            case "look" :: argumentWords => lookAt(character, argumentWords)

    private def lookAtRoom(character: Mobile) = {
        val room = character.outside
        val otherChars = room.mobiles diff Seq(character)
        val exits = joinOrElse(room.exits.keys map (_.toString), ", ", "none")
        val titles = collapseDuplicates(room.contents map (unitDisplay(_))) ++ collapseDuplicates(otherChars map (unitDisplay(_)))
        CommandResult(
            sendMessageToCharacter(
                character,
                s"""$$BrightWhite${room.title}$$Reset
                   |$$s3${room.description}
                   |$$BrightYellowExits: $exits$$Reset
                   |${titles mkString "\n"}""".stripMargin),
            addMiniMap = true)
    }

    private def lookInside(character: Mobile, argumentWords: List[String]) = {
        val recipients = character.findItemInOrNextToMe(argumentWords mkString " ") match
            case None => sendMessageToCharacter(character, "No such thing here to look inside.")
            case Some(unitToLookAt) =>
                val itemsDisplay = joinOrElse(collapseDuplicates(unitToLookAt.contents map (unitDisplay(_))))
                sendMessageToCharacter(character,
                    s"""You look inside the ${unitToLookAt.name}. It contains:
                       |$itemsDisplay""".stripMargin)
        CommandResult(recipients)
    }

    private def lookAt(character: Mobile, argumentWords: Seq[String]) = {
        val recipients = character.findInOrNextToMe(argumentWords mkString " ") match
            case None => sendMessageToCharacter(character, "No such thing here to look at.")
            case Some(itemToLookAt: Item) =>
                sendMessageToCharacter(
                    character,
                    s"""You look at the ${itemToLookAt.name}.
                       |${itemToLookAt.description}""".stripMargin)
            case Some(characterToLookAt: Mobile) =>
                val equipmentDisplay = getEquipmentDisplay(characterToLookAt) mkString "\n"
                sendMessageToCharacter(
                    character,
                    s"""You look at the ${characterToLookAt.name}.
                       |${characterToLookAt.description}
                       |$equipmentDisplay""".stripMargin)
        CommandResult(recipients)
    }
