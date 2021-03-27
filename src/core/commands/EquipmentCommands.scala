package core.commands

import core.{Character, FindInInventory, FindInOrNextToMe, FindNextToMe}
import core.GameUnit.findUnit
import core.commands.Commands.{joinOrElse, sendMessage}

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
                                sendMessage(character, s"You ${commandWords.head} the ${target.name} from the ${container.name}.")
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
                        sendMessage(character, s"You ${commandWords.head} the ${target.name}.")
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
                sendMessage(character, s"You drop your ${target.name}.")
            }
            case None => sendMessage(character, "You don't seem to have any such thing.")
        }
    }
}
