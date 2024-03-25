package core

import core.ActRecipient.*
import core.MiniMap.*
import core.connection.Output
import core.gameunit.*
import core.gameunit.Gender.*
import core.util.MessagingUtils.unitDisplay

import scala.Array.tabulate

class MessageSender:

    def sendMessage(character: Mobile, message: String, addMap: Boolean = false) =

        // TODO: don't send prompt until all messages have been sent
        // TODO: don't send prompt after quit
        val textWidth = 50

        // TODO: exclude color codes when counting width
        // TODO: hyphenation?
        val formattedOutput = message.linesIterator map (_ grouped textWidth mkString "\n") mkString "\n"

        val prompt = "(12/20) fake-prompt (12/20)"

        val formattedOutputWithPromptAndMap =
            (addMap, character.outside) match {
                case (true, room: Room) =>

                    val mapLines = colourMiniMap(frameMiniMap(miniMap(room, 3)))
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
                case _ => formattedOutput + "\n\n" + prompt
            }

        character match
            case pc: PlayerCharacter => pc.connection.write(Output(formattedOutput + "\n\n" + prompt, ""))
            case _ => // TODO: send to controlling admin
    end sendMessage

    def act(message: String, visibility: ActVisibility,
            actor: Option[Mobile], medium: Option[Findable], target: Option[Findable],
            toWhom: ActRecipient, text: Option[String]) =

        def charactersInRoom =
            actor map (_.outside.mobiles) getOrElse Seq()

        def isSame(unitA: GameUnit, unitB: Option[GameUnit]) =
            unitB contains unitA

        val recipients =
            toWhom match
                case ToActor => actor.toSeq
                case ToTarget => target match
                    case Some(m: Mobile) => Seq(m)
                    case _ => Seq()
                case ToBystanders => charactersInRoom filterNot (isSame(_, actor)) filterNot (isSame(_, target))
                case ToAllExceptActor => charactersInRoom filterNot (isSame(_, actor))
                case ToEntireRoom => charactersInRoom

        def formatGender(unit: Findable, genderToNoun: Gender => String) =
            unit match
                case character: Mobile => genderToNoun(character.gender)
                case _ => genderToNoun(GenderNeutral)

        def formatUnit(unit: Findable, formatter: String) =
            formatter match // TODO: visibility
                case "a" => if Set('a', 'e', 'i', 'o') contains unit.name.head then "an" else "a"
                case "e" => formatGender(unit, _.e)
                case "m" => formatGender(unit, _.m)
                case "s" => formatGender(unit, _.s)
                case "n" => unitDisplay(unit, includePlayerTitle = false)
                case "N" => unit.name
                case "p" => "unit.position" // TODO: positions
                case "t" => text getOrElse "null"
                case _ => "invalidFormatter"

        val nounPattern = "\\$([1-3])([aemsnNpt])".r
        val recipientUnits = Array(actor, medium, target)

        def replacement(msg: String) = nounPattern.replaceAllIn(msg, _ match
            case nounPattern(unitIndex, formatter) =>
                recipientUnits(unitIndex.toInt - 1)
                    .map(formatUnit(_, formatter))
                    .getOrElse("null"))

        recipients foreach (sendMessage(_, replacement(message).capitalize))
    end act


object MessageSender:
    given MessageSender = new MessageSender

enum ActRecipient:
    case ToActor, ToTarget, ToBystanders, ToAllExceptActor, ToEntireRoom

enum ActVisibility:
    case Always, Someone, HideInvisible
