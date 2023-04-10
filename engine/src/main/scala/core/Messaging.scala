package core

import core.ActRecipient.*
import core.ActVisibility.*
import core.MiniMap.*
import core.gameunit.*
import core.gameunit.Gender.*

import scala.Array.tabulate
import scala.collection.mutable.ListBuffer

object Messaging:

    def sendMessage(character: Character, message: String, addMap: Boolean = false) =

        // TODO: don't send prompt until all messages have been sent
        // TODO: don't send prompt after quit
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

        val nounPattern    = "\\$([1-3])([aemsnNpt])".r
        val recipientUnits = Array(actor, medium, target)

        def replacement(msg: String) = nounPattern.replaceAllIn(msg, _ match {
            case nounPattern(unitIndex, formatter) => recipientUnits(unitIndex.toInt - 1)
                .map(formatUnit(_, formatter))
                .getOrElse("null")
        })

        recipients foreach (sendMessage(_, replacement(message).capitalize))
    end act

    def joinOrElse(strings: Iterable[String], separator: String, default: String) =
        Option(strings)
            .filterNot(_.isEmpty)
            .map(_ mkString separator)
            .getOrElse(default)

    def mapContent(unit: GameUnit, includePlayerTitle: Boolean = true) =
        (unit, includePlayerTitle) match {
            case (player: PlayerCharacter, true)  => player.name + " " + player.title
            case (player: PlayerCharacter, false) => player.name
            case _                                => unit.title
        }


enum ActRecipient:
    case ToActor, ToTarget, ToBystanders, ToAllExceptActor, ToEntireRoom


enum ActVisibility:
    case Always, Someone, HideInvisible
