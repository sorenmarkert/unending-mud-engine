package core.commands

import core.ActVisibility.*
import core.MessageSender
import core.commands.Commands.CommandResult
import core.gameunit.*
import core.util.MessagingUtils.*

class EquipmentCommands(using messageSender: MessageSender):

    import messageSender.*

    private[commands] def inventory(character: Mobile, commandWords: Seq[String]): CommandResult =
        val titles = joinOrElse(character.inventory map (_.title))
        CommandResult(sendMessageToCharacter(character, "You are carrying:\n" + titles))

    private[commands] def equipment(character: Mobile, commandWords: Seq[String]): CommandResult =
        val titles = joinOrElse(getEquipmentDisplay(character))
        CommandResult(sendMessageToCharacter(character, "$BrightBlueYou are using:\n" + titles + "$Reset"))

    private[commands] def get(character: Mobile, commandWords: Seq[String]): CommandResult =
        val recipients = commandWords splitAt (commandWords indexOf "from") match
            case (_ :: targetWords, "from" :: containerWords) =>
                character.findItemInOrNextToMe(containerWords mkString " ") match
                    case None => sendMessageToCharacter(character, s"No such thing here to ${commandWords.head} things from.")
                    case Some(container) =>
                        container.findInside(targetWords mkString " ") match
                            case None => sendMessageToCharacter(character, s"The ${container.name} does not seem to contain such a thing.")
                            case Some(target) =>
                                character.addItem(target)
                                act("$1N $[get|gets] $2n from $3n.", Always, Some(character), Some(target), Some(container))

            case (Nil, _ :: targetWords) =>
                character.findItemNextToMe(targetWords mkString " ") match
                    case None => sendMessageToCharacter(character, s"No such thing here to ${commandWords.head}.")
                    case Some(target) =>
                        character.addItem(target)
                        act("$1N $[get|gets] $2n.", Always, Some(character), Some(target))

            case _ => sendMessageToCharacter(character, commandWords.head + " 'what' [from 'what'] ?")
        CommandResult(recipients)
    end get

    private[commands] def drop(character: Mobile, commandWords: Seq[String]): CommandResult =
        val recipients = character.findInInventory(commandWords.tail mkString " ") match
            case None => sendMessageToCharacter(character, "You don't seem to have any such thing.")
            case Some(target) =>
                character.outside.addItem(target)
                act("$1N $[drop|drops] $1s $2N.", Always, Some(character), Some(target))
        CommandResult(recipients)

    private[commands] def put(character: Mobile, commandWords: Seq[String]): CommandResult =
        val recipients = commandWords splitAt (commandWords indexOf "in") match
            case (_ :: targetWords, "in" :: containerWords) =>
                val mediumOption = character.findInInventory(targetWords mkString " ")
                val containerOption = character.findItemInOrNextToMe(containerWords mkString " ")
                (mediumOption, containerOption) match
                    case (Some(target), Some(container)) if target == container =>
                        sendMessageToCharacter(character, s"You fail to ${commandWords.head} the ${target.name} into itself...")
                    case (Some(target), Some(container)) =>
                        container.addItem(target)
                        act("$1N $[put|puts] $1s $2N in $3n.", Always, Some(character), Some(target), Some(container))
                    case (None, _) => sendMessageToCharacter(character, s"You don't have any such thing on you.")
                    case _ => sendMessageToCharacter(character, s"No such thing here to ${commandWords.head} things in.")
            case _ => sendMessageToCharacter(character, commandWords.head + " 'what' in 'what' ?")
        CommandResult(recipients)
    end put

    private[commands] def give(character: Mobile, commandWords: Seq[String]): CommandResult =
        val recipients = commandWords splitAt (commandWords indexOf "to") match
            case (_ :: mediumWords, "to" :: targetWords) =>
                val mediumOption = character.findInInventory(mediumWords mkString " ")
                val targetOption = character.findMobile(targetWords mkString " ")
                (mediumOption, targetOption) match
                    case (Some(medium), Some(target)) =>
                        target.addItem(medium)
                        act("$1n $[give|gives] $2n to $3n.", Always, Some(character), Some(medium), Some(target))
                    case (None, _) =>
                        sendMessageToCharacter(character, s"You don't have any such thing on you.")
                    case _ =>
                        sendMessageToCharacter(character, s"There's nobody here by that name to ${commandWords.head} things to.")
            case _ => sendMessageToCharacter(character, commandWords.head + " 'what' to 'whom' ?")
        CommandResult(recipients)

    private[commands] def examine(character: Mobile, commandWords: Seq[String]): CommandResult =
        val recipients = commandWords match
            case "examine" :: Nil => sendMessageToCharacter(character, "Examine 'what'?")
            case "examine" :: argumentWords =>
                character.findItemInOrNextToMe(argumentWords mkString " ") match
                    case None => sendMessageToCharacter(character, "No such thing here to look inside.")
                    case Some(itemToExamine) =>
                        val itemsDisplay = joinOrElse(collapseDuplicates(itemToExamine.contents map (unitDisplay(_))))
                        sendMessageToCharacter(
                            character,
                            s"""You examine the ${itemToExamine.name}.
                               |${itemToExamine.description}
                               |It contains:
                               |$itemsDisplay""".stripMargin)
        CommandResult(recipients)

    private[commands] def wear(character: Mobile, commandWords: Seq[String]): CommandResult =
        val recipients = character.findInInventory(commandWords.tail mkString " ") match
            case None => sendMessageToCharacter(character, "You don't seem to have any such thing.")
            case Some(target) =>
                character.equip(target) match
                    case None => act("$1N $[wear|wears] $1s $2N.", Always, Some(character), Some(target))
                    case Some(errorMessage) => sendMessageToCharacter(character, errorMessage)
        CommandResult(recipients)

    private[commands] def remove(character: Mobile, commandWords: Seq[String]): CommandResult =
        val recipients = character.findInEquipped(commandWords.tail mkString " ") match
            case None => sendMessageToCharacter(character, "You don't seem to have any such thing equipped.")
            case Some(target) =>
                character.remove(target) match
                    case None => act("$1N $[remove|removes] $1s $2N.", Always, Some(character), Some(target))
                    case Some(errorMessage) => sendMessageToCharacter(character, errorMessage)
        CommandResult(recipients)


object EquipmentCommands:
    given EquipmentCommands = EquipmentCommands()
