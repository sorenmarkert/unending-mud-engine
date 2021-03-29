package core.commands

import core.{Character, FindInInventory, FindInOrNextToMe, FindNextToMe}
import core.GameUnit.findUnit
import core.commands.Commands.{act, joinOrElse, sendMessage}

object EquipmentCommands {

    private[commands] def inventory(character: Character, commandWords: Seq[String]) = {
        val titles = joinOrElse(character.inventory map (_.title), "\n", "Nothing.")
        sendMessage(character, "You are carrying:\n" + titles)
    }

    private[commands] def equipment(character: Character, commandWords: Seq[String]) = {
        // TODO: item slots
        val titles = joinOrElse(character.equippedItems map (_.title), "\n", "Nothing.")
        sendMessage(character, "You are using:\n" + titles)
    }

    private[commands] def get(character: Character, commandWords: Seq[String]) = {

        commandWords splitAt (commandWords indexOf "from") match {
            case (_ :: targetWords, "from" :: containerWords) => {
                findUnit(character, containerWords mkString " ", Left(FindInOrNextToMe)) match {
                    case Some(container) => {
                        findUnit(character, targetWords mkString " ", Right(container)) match {
                            case Some(target) => {
                                character addUnit target
                                act("You $1t $2n from $3n.", Always,
                                    Some(character), Some(target), Some(container), ToActor, Some(commandWords.head))
                                act("$1n $1ts $2n from $3n.", Always,
                                    Some(character), Some(target), Some(container), ToAllExceptActor, Some(commandWords.head))
                            }
                            case None => sendMessage(character, s"The ${container.name} does not seem to contain such a thing.")
                        }
                    }
                    case None => sendMessage(character, s"No such thing here to ${commandWords.head} things from.")
                }
            }

            case (Nil, _ :: targetWords) => {
                findUnit(character, targetWords mkString " ", Left(FindNextToMe)) match {
                    case Some(target) => {
                        character addUnit target
                        act("You $1t $2n.", Always, Some(character), Some(target), None, ToActor, Some(commandWords.head))
                        act("$1n $1ts $2n.", Always, Some(character), Some(target), None, ToAllExceptActor, Some(commandWords.head))
                    }
                    case None => sendMessage(character, s"No such thing here to ${commandWords.head}.")
                }
            }

            case _ => sendMessage(character, commandWords.head + " 'what' [from 'what'] ?")
        }
    }

    private[commands] def drop(character: Character, commandWords: Seq[String]) = {
        findUnit(character, commandWords.tail mkString " ", Left(FindInInventory)) match {
            case Some(target) => {
                character.outside foreach (_ addUnit target)
                act("You drop your $2N.", Always, Some(character), Some(target), None, ToActor, None)
                act("$1n drops $1s $2N.", Always, Some(character), Some(target), None, ToAllExceptActor, None)
            }
            case None => sendMessage(character, "You don't seem to have any such thing.")
        }
    }

    private[commands] def put(character: Character, commandWords: Seq[String]) = {

        commandWords splitAt (commandWords indexOf "in") match {
            case (_ :: targetWords, "in" :: containerWords) => {
                val targetOption = findUnit(character, targetWords mkString " ", Left(FindInInventory))
                val containerOption = findUnit(character, containerWords mkString " ", Left(FindInOrNextToMe))
                (targetOption, containerOption) match {
                    case (Some(target), Some(container)) if target.uuid == container.uuid =>
                        sendMessage(character, s"You fail to ${commandWords.head} your ${target.name} into itself...")
                    case (Some(target), Some(container)) => {
                        container addUnit target
                        act("You $1t your $2N in $3n.", Always,
                            Some(character), Some(target), Some(container), ToActor, Some(commandWords.head))
                        act("$1n $1ts $1s $2N in $3n.", Always,
                            Some(character), Some(target), Some(container), ToAllExceptActor, Some(commandWords.head))
                    }
                    case (None, _) => sendMessage(character, s"You don't have any such thing on you.")
                    case _ => sendMessage(character, s"No such thing here to ${commandWords.head} things in.")
                }
            }
            case _ => sendMessage(character, commandWords.head + " 'what' in 'what' ?")
        }
    }
}
