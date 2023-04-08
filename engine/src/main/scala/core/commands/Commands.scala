package core.commands

import core.*
import core.GlobalState.actorSystem
import core.MiniMap.*
import core.StateActor.CommandExecution
import core.commands.ActRecipient.*
import core.commands.ActVisibility.*
import core.commands.BasicCommands.*
import core.commands.CombatCommands.{doSlash, prepareSlash}
import core.commands.EquipmentCommands.*
import core.gameunit.Gender.*
import core.gameunit.{Character, GameUnit, Gender, PlayerCharacter, Room}

import scala.Array.tabulate
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}

sealed trait Command


case class InstantCommand(func: (Character, Seq[String]) => Unit, canInterrupt: Boolean = false) extends Command


case class TimedCommand(baseDuration: FiniteDuration,
                        beginFunc   : (Character, Seq[String]) => Option[String],
                        endFunc     : (Character, Seq[String]) => Unit) extends Command


object Commands:

    private val nounPattern = "\\$([1-3])([aemsnNpt])".r

    private val emptyInput     = InstantCommand((char, _) => sendMessage(char, ""))
    private val unknownCommand = InstantCommand((char, _) => sendMessage(char, "What's that?"))

    private val commandList: Seq[(String, Command)] = Seq(
        "quit" -> InstantCommand(quit),

        "north" -> InstantCommand(movement),
        "south" -> InstantCommand(movement),
        "east" -> InstantCommand(movement),
        "west" -> InstantCommand(movement),
        "up" -> InstantCommand(movement),
        "down" -> InstantCommand(movement),

        "look" -> InstantCommand(look, canInterrupt = true),
        "minimap" -> InstantCommand(minimap, canInterrupt = true),

        "inventory" -> InstantCommand(inventory, canInterrupt = true),
        "equipment" -> InstantCommand(equipment, canInterrupt = true),
        "examine" -> InstantCommand(examine, canInterrupt = true),
        "get" -> InstantCommand(get),
        "take" -> InstantCommand(get),
        "drop" -> InstantCommand(drop),
        "put" -> InstantCommand(put),
        "place" -> InstantCommand(put),
        "give" -> InstantCommand(give),
        "wear" -> InstantCommand(wear),
        "remove" -> InstantCommand(remove),

        "slash" -> TimedCommand(2.seconds, prepareSlash, doSlash),
        )

    def executeCommand(character: Character, input: String): String =

        val inputWords = (input split " ").toList filterNot (_.isBlank)

        val (command, commandWords) =
            inputWords match {
                case "" :: _ | Nil              => (emptyInput, Nil)
                case commandPrefix :: arguments =>
                    commandList
                        .find { case (k, _) => k startsWith commandPrefix.toLowerCase } // TODO: use a trie
                        .map { case (commandString, command) => (command, commandString :: arguments) }
                        .getOrElse((unknownCommand, Nil))
            }

        actorSystem tell CommandExecution(command, character, commandWords)

        commandWords.headOption getOrElse ""

    def sendMessage(character: Character, message: String, addMap: Boolean = false) =

        // TODO: don't send prompt until all messages have been sent
        val textWidth = 50

        // TODO: exclude color codes when counting width
        // TODO: hyphenation
        val formattedOutput = message.linesIterator map (_ grouped textWidth mkString "\n") mkString "\n"

        val prompt = "(12/20) fake-prompt (12/20)"

        val formattedOutputWithPromptAndMap =
            (addMap, character.outside) match {
                case (true, Some(room: Room)) =>

                    val mapLines    = colourMiniMap(frameMiniMap(miniMap(room, 3)))
                    val promptLines = (prompt grouped textWidth).toList

                    formattedOutput
                        .linesIterator
                        .padTo(mapLines.size - promptLines.size, "")
                        .toList
                        .appendedAll(promptLines)
                        .map(_.padTo(textWidth, ' '))
                        .zipAll(
                            mapLines,
                            tabulate(textWidth)(_ => ' ').mkString,
                            "")
                        .map(a => a._1 + "  " + a._2)
                        .mkString("\n")
                case _                        => formattedOutput + "\n\n" + prompt
            }

        character match {
            case PlayerCharacter(_, connection) => connection.write(formattedOutputWithPromptAndMap + "\u001b[0m")
            case _                              => // TODO: send to controlling admin
        }

    end sendMessage

    def act(message: String, visibility: ActVisibility,
            actor  : Option[GameUnit], medium: Option[GameUnit], target: Option[GameUnit],
            toWhom : ActRecipient, text: Option[String]) =

        def toCharacter(unit: GameUnit) =
            unit match {
                case character: Character => Some(character)
                case _                    => None
            }

        def charactersInRoom =
            actor map (_.outside.get.contents flatMap toCharacter) getOrElse ListBuffer()

        def isSame(unitA: GameUnit, unitB: Option[GameUnit]) =
            unitB contains unitA

        val recipients =
            toWhom match {
                case ToActor          => (actor flatMap toCharacter).toList
                case ToTarget         => (target flatMap toCharacter).toList
                case ToBystanders     => charactersInRoom filterNot (isSame(_, actor)) filterNot (isSame(_, target))
                case ToAllExceptActor => charactersInRoom filterNot (isSame(_, actor))
                case ToEntireRoom     => charactersInRoom
            }

        def formatGender(unit: GameUnit, genderToNoun: Gender => String) =
            unit match {
                case character: Character => genderToNoun(character.gender)
                case _                    => genderToNoun(GenderNeutral)
            }

        def formatUnit(unit: GameUnit, formatter: String) =
            formatter match { // TODO: visibility
                case "a" => if Set('a', 'e', 'i', 'o') contains unit.name.head then "an" else "a"
                case "e" => formatGender(unit, _.e)
                case "m" => formatGender(unit, _.m)
                case "s" => formatGender(unit, _.s)
                case "n" => mapContent(unit, includePlayerTitle = false)
                case "N" => unit.name
                case "p" => "unit.position" // TODO: positions
                case "t" => text getOrElse "null"
                case _   => "invalidFormatter"
            }

        val units = Array(actor, medium, target)

        def replacement(msg: String) = nounPattern.replaceAllIn(msg, _ match {
            case nounPattern(unitIndex, formatter) => units(unitIndex.toInt - 1)
                .map(formatUnit(_, formatter))
                .getOrElse("null")
        })

        recipients foreach (sendMessage(_, firstCharToUpper(replacement(message))))

    end act

    private[commands] def joinOrElse(strings: Iterable[String], separator: String, default: String) =
        Option(strings)
            .filterNot(_.isEmpty)
            .map(_ mkString separator)
            .getOrElse(default)

    private[commands] def mapContent(unit: GameUnit, includePlayerTitle: Boolean = true) =
        (unit, includePlayerTitle) match {
            case (player: PlayerCharacter, true)  => player.name + " " + player.title
            case (player: PlayerCharacter, false) => player.name
            case _                                => unit.title
        }

    private[commands] def firstCharToUpper(message: String) =
        s"${message.head.toUpper}${message.tail}"

end Commands


enum ActRecipient:
    case ToActor, ToTarget, ToBystanders, ToAllExceptActor, ToEntireRoom


enum ActVisibility:
    case Always, Someone, HideInvisible
