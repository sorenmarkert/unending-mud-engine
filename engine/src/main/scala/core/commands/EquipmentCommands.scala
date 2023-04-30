package core.commands

import core.ActRecipient.*
import core.ActVisibility.*
import core.MessageSender
import core.MessageSender.*
import core.gameunit.*
import core.util.MessagingUtils.*

class EquipmentCommands(using messageSender: MessageSender):

    import messageSender.*

    private[commands] def inventory(character: Mobile, commandWords: Seq[String]) =
        val titles = joinOrElse(character.inventory map (_.title), "\n", "Nothing.")
        sendMessage(character, "You are carrying:\n" + titles)

    private[commands] def equipment(character: Mobile, commandWords: Seq[String]) =
        // TODO: display item slots
        val titles = joinOrElse(character.equippedItems map (_.title), "\n", "Nothing.")
        sendMessage(character, "You are using:\n" + titles)

    private[commands] def get(character: Mobile, commandWords: Seq[String]) =

        commandWords splitAt (commandWords indexOf "from") match {
            case (_ :: targetWords, "from" :: containerWords) =>
                character.findItemInOrNextToMe(containerWords mkString " ") match {
                    case Some(container) =>
                        container.findInside(targetWords mkString " ") match {
                            case Some(target) =>
                                character addItem target
                                act("You $1t $2n from $3n.", Always,
                                    Some(character), Some(target), Some(container), ToActor, Some(commandWords.head))
                                act("$1N $1ts $2n from $3n.", Always,
                                    Some(character), Some(target), Some(container), ToAllExceptActor, Some(commandWords.head))
                            case None         => sendMessage(character, s"The ${container.name} does not seem to contain such a thing.")
                        }
                    case None            => sendMessage(character, s"No such thing here to ${commandWords.head} things from.")
                }

            case (Nil, _ :: targetWords) =>
                character.findItemNextToMe(targetWords mkString " ") match {
                    case Some(target) =>
                        character addItem target
                        act("You $1t $2n.", Always, Some(character), Some(target), None, ToActor, Some(commandWords.head))
                        act("$1n $1ts $2n.", Always, Some(character), Some(target), None, ToAllExceptActor, Some(commandWords.head))
                    case None         => sendMessage(character, s"No such thing here to ${commandWords.head}.")
                }

            case _ => sendMessage(character, commandWords.head + " 'what' [from 'what'] ?")
        }
    end get

    private[commands] def drop(character: Mobile, commandWords: Seq[String]) =
        character.findInInventory(commandWords.tail mkString " ") match {
            case Some(target) =>
                character.outside addItem target
                act("You drop your $2N.", Always, Some(character), Some(target), None, ToActor, None)
                act("$1N drops $1s $2N.", Always, Some(character), Some(target), None, ToAllExceptActor, None)
            case None         => sendMessage(character, "You don't seem to have any such thing.")
        }

    private[commands] def put(character: Mobile, commandWords: Seq[String]) =
    // TODO: check if can contain
        commandWords splitAt (commandWords indexOf "in") match {
            case (_ :: targetWords, "in" :: containerWords) =>
                val mediumOption    = character.findInInventory(targetWords mkString " ")
                val containerOption = character.findItemInOrNextToMe(containerWords mkString " ")
                (mediumOption, containerOption) match {
                    case (Some(target), Some(container)) if target == container =>
                        sendMessage(character, s"You fail to ${commandWords.head} the ${target.name} into itself...")
                    case (Some(target), Some(container))                        =>
                        container addItem target
                        act("You $1t your $2N in $3n.", Always,
                            Some(character), Some(target), Some(container), ToActor, Some(commandWords.head))
                        act("$1N $1ts $1s $2N in $3n.", Always,
                            Some(character), Some(target), Some(container), ToAllExceptActor, Some(commandWords.head))
                    case (None, _)                                              => sendMessage(character, s"You don't have any such thing on you.")
                    case _                                                      => sendMessage(character, s"No such thing here to ${commandWords.head} things in.")
                }
            case _                                          => sendMessage(character, commandWords.head + " 'what' in 'what' ?")
        }
    end put

    private[commands] def give(character: Mobile, commandWords: Seq[String]) = {
        // TODO: check if can carry
        commandWords splitAt (commandWords indexOf "to") match {
            case (_ :: mediumWords, "to" :: targetWords) =>
                val mediumOption = character.findInInventory(mediumWords mkString " ")
                val targetOption = character.findMobile(targetWords mkString " ")
                (mediumOption, targetOption) match {
                    case (Some(medium), Some(target)) =>
                        target addItem medium
                        act("You $1t $2n to $3n.", Always,
                            Some(character), Some(medium), Some(target), ToActor, Some(commandWords.head))
                        act("$1n $1ts you $2n.", Always,
                            Some(character), Some(medium), Some(target), ToTarget, Some(commandWords.head))
                        act("$1n $1ts $2n to $3n.", Always,
                            Some(character), Some(medium), Some(target), ToBystanders, Some(commandWords.head))
                    case (None, _)                    =>
                        sendMessage(character, s"You don't have any such thing on you.")
                    case _                            =>
                        sendMessage(character, s"There's nobody here by that name to ${commandWords.head} things to.")
                }
            case _                                       => sendMessage(character, commandWords.head + " 'what' to 'whom' ?")
        }
    }

    private[commands] def examine(character: Mobile, commandWords: Seq[String]) =
        commandWords match {
            case "examine" :: Nil           => sendMessage(character, "Examine 'what'?")
            case "examine" :: argumentWords =>
                character.findItemInOrNextToMe(argumentWords mkString " ") match {
                    case Some(itemToExamine) =>
                        sendMessage(character, "You examine the %s.\n%s\nIt contains:\n%s".format(
                            itemToExamine.name,
                            itemToExamine.description,
                            joinOrElse(collapseDuplicates(itemToExamine.contents map (unitDisplay(_))), "\n", "Nothing.")))
                    case None                => sendMessage(character, "No such thing here to look inside.")
                }
        }

    private[commands] def wear(character: Mobile, commandWords: Seq[String]) =
        character.findInInventory(commandWords.tail mkString " ") match {
            case Some(target) =>
                character equip target match {
                    case None               =>
                        act("You wear your $2N.", Always, Some(character), Some(target), None, ToActor, None)
                        act("$1N wears $1s $2N.", Always, Some(character), Some(target), None, ToAllExceptActor, None)
                    case Some(errorMessage) => sendMessage(character, errorMessage)
                }
            case _            => sendMessage(character, "You don't seem to have any such thing.")
        }

    private[commands] def remove(character: Mobile, commandWords: Seq[String]) =
        character.findInEquipped(commandWords.tail mkString " ") match {
            case Some(target) =>
                character remove target match {
                    case None               =>
                        act("You remove your $2N.", Always, Some(character), Some(target), None, ToActor, None)
                        act("$1N removes $1s $2N.", Always, Some(character), Some(target), None, ToAllExceptActor, None)
                    case Some(errorMessage) => sendMessage(character, errorMessage)
                }
            case _            => sendMessage(character, "You don't seem to have any such thing equipped.")
        }

end EquipmentCommands