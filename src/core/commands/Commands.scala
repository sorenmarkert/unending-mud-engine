package core.commands

import core.commands.BasicCommands.{look, movement, quit}
import core.commands.EquipmentCommands._
import core.{Character, GameUnit, Item, PlayerCharacter, Room, Gender, GenderFemale, GenderMale, GenderNeutral}

import scala.collection.mutable.ListBuffer

sealed trait Command

case class InstantCommand(func: (Character, Seq[String]) => Unit) extends Command

case class DurationCommand(duration: Int,
                           beginFunc: (Character, Seq[String]) => Option[String],
                           endFunc: (Character, Seq[String]) => Unit) extends Command

object Commands {

    private val pattern = "\\$([1-3])([aemsnNpt])".r

    private val emptyInput = InstantCommand((char, _) => sendMessage(char, ""))
    private val unknownCommand = InstantCommand((char, _) => sendMessage(char, "What's that?"))

    private val commandMap = Map(
        "quit" -> InstantCommand(quit),

        "north" -> InstantCommand(movement),
        "south" -> InstantCommand(movement),
        "east" -> InstantCommand(movement),
        "west" -> InstantCommand(movement),
        "up" -> InstantCommand(movement),
        "down" -> InstantCommand(movement),

        "look" -> InstantCommand(look),
        "inventory" -> InstantCommand(inventory),
        "equipment" -> InstantCommand(equipment),
        "get" -> InstantCommand(get),
        "take" -> InstantCommand(get),
        "drop" -> InstantCommand(drop),
        "put" -> InstantCommand(put),
        "place" -> InstantCommand(put),
    )

    def executeCommand(character: Character, input: String) = {

        val inputWords = input.split(" ").toList

        val (command, argument) = inputWords match {
            case "" :: _ | Nil => (emptyInput, Nil)
            case commandPrefix :: arguments => commandMap
                .keys
                .find(_ startsWith commandPrefix) // TODO: use a trie
                .map(commandString => (commandMap(commandString), commandString :: arguments filterNot (_.isBlank)))
                .getOrElse((unknownCommand, Nil))
        }

        command.func(character, argument)
    }

    def sendMessage(character: Character, message: String) = character match {
        case PlayerCharacter(_, writer) => writer println (message + "\n\n(12/20)fake-prompt(12/20)")
        case _ =>
    }

    def act(message: String, visibility: ActVisibility, actor: Option[GameUnit], medium: Option[GameUnit],
            target: Option[GameUnit], toWhom: ActRecipient, text: Option[String]) = {

        def toCharacter(unit: GameUnit) = unit match {
            case character: Character => Some(character)
            case _ => None
        }

        def charactersInRoom = actor map (_.outside.get.contents flatMap toCharacter) getOrElse ListBuffer()

        def isSame(unitA: GameUnit, unitB: Option[GameUnit]) = unitB exists (_.uuid == unitA.uuid)

        val recipients = toWhom match {
            case ToActor => (actor flatMap toCharacter).toList
            case ToTarget => (target flatMap toCharacter).toList
            case ToBystanders => charactersInRoom filterNot (isSame(_, actor)) filterNot (isSame(_, target))
            case ToAllExceptActor => charactersInRoom filterNot (isSame(_, actor))
            case ToEntireRoom => charactersInRoom
        }

        def formatGender(unit: GameUnit, genderMap: Map[Gender, String]) = unit match {
            case character: Character => genderMap(character.gender)
            case _ => genderMap(GenderNeutral)
        }

        def formatUnit(unit: GameUnit, formatter: String) = formatter match { // TODO: visibility
            case "a" => if (Set('a', 'e', 'i', 'o') contains unit.name.head) "an" else "a"
            case "e" => formatGender(unit, Map(GenderMale -> "he", GenderFemale -> "she", GenderNeutral -> "it"))
            case "m" => formatGender(unit, Map(GenderMale -> "him", GenderFemale -> "her", GenderNeutral -> "it"))
            case "s" => formatGender(unit, Map(GenderMale -> "his", GenderFemale -> "her", GenderNeutral -> "its"))
            case "n" => unit.title
            case "N" => unit.name
            case "p" => "unit.position" // TODO: positions
            case "t" => text getOrElse "null"
            case _ => "invalidFormatter"
        }

        val units = Array(actor, medium, target)

        def replacement(msg: String) = pattern.replaceAllIn(msg, _ match {
            case pattern(unitIndex, formatter) => units(unitIndex.toInt - 1)
                .map(formatUnit(_, formatter))
                .getOrElse("null")
        })

        recipients foreach (sendMessage(_, replacement(message)))
    }

    private[commands] def joinOrElse(strings: Iterable[String], separator: String, default: String) = {
        Option(strings)
            .filterNot(_.isEmpty)
            .map(_ mkString separator)
            .getOrElse(default)
    }
}


sealed trait ActRecipient

case object ToActor extends ActRecipient

case object ToTarget extends ActRecipient

case object ToBystanders extends ActRecipient

case object ToAllExceptActor extends ActRecipient

case object ToEntireRoom extends ActRecipient


sealed trait ActVisibility

case object Always extends ActVisibility

case object Someone extends ActVisibility

case object HideInvisible extends ActVisibility
