package core

import core.ActRecipient.*
import core.MiniMap.*
import core.gameunit.*
import core.gameunit.Gender.*
import core.util.MessagingUtils.{groupedIgnoringColourCodes, substituteColours, unitDisplay}

class MessageSender:

    val textWidth = 50

    def sendMessage(character: Mobile, message: String, addMiniMap: Boolean = false, addPrompt: Boolean = true) =

        lazy val formattedMessageLines =
            message
                .capitalize
                .linesIterator
                .flatMap(groupedIgnoringColourCodes(_, textWidth))
                .toSeq

        character match
            case pc: PlayerCharacter =>
                pc.connection.enqueueMessage(
                    formattedMessageLines.map(substituteColours(_, pc.connection.substituteColourCodes)))
                Set(pc)
            case _ => Set() // TODO: send to controlling admin
    end sendMessage

    def sendAllEnqueuedMessages(character: Mobile, addMiniMap: Boolean = false, addPrompt: Boolean = true): Unit =

        val prompt = "(12/20) fake-prompt (12/20)"
        val promptLines = if addPrompt then groupedIgnoringColourCodes(prompt, textWidth).toSeq else Seq()

        val mapLines = (addMiniMap, character.outside) match
            case (true, room: Room) => colourMiniMap(frameMiniMap(miniMap(room, 3)))
            case _ => Seq()

        character match
            case pc: PlayerCharacter => pc.connection.sendEnqueuedMessages(promptLines, mapLines)
            case _ => // TODO: send to controlling admin

    // TODO: can we get rid of toWhom, and format $[split, splits] instead? with $1n and $3n becoming 'you'?
    def act(message: String, visibility: ActVisibility,
            actor: Option[Mobile], medium: Option[Findable], target: Option[Findable],
            toWhom: ActRecipient, text: Option[String]): Set[PlayerCharacter] =

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
                case "e" => formatGender(unit, _.subject)
                case "m" => formatGender(unit, _.obJect)
                case "s" => formatGender(unit, _.possessive)
                case "n" => unitDisplay(unit, includePlayerTitle = false)
                case "N" => unit.name
                case "p" => "unit.position" // TODO: positions
                case "t" => text getOrElse "null"
                case _ => "[invalidFormatter]"

        val nounPattern = "\\$([1-3])([aemsnNpt])".r
        val recipientUnits = Array(actor, medium, target)

        def replaceUnits(msg: String) = nounPattern.replaceAllIn(msg, _ match
            case nounPattern(unitIndex, formatter) =>
                recipientUnits(unitIndex.toInt - 1)
                    .map(formatUnit(_, formatter))
                    .getOrElse("[null]"))

        recipients.flatMap(sendMessage(_, replaceUnits(message))).toSet
    end act


object MessageSender:
    given MessageSender = new MessageSender


enum ActRecipient:
    case ToActor, ToTarget, ToBystanders, ToAllExceptActor, ToEntireRoom

enum ActVisibility:
    case Always, Someone, HideInvisible

enum Colour:
    case Black, BrightBlack
    case Red, BrightRed
    case Green, BrightGreen
    case Yellow, BrightYellow
    case Blue, BrightBlue
    case Magenta, BrightMagenta
    case Cyan, BrightCyan
    case White, BrightWhite
    case Reset

object Colour:
    val colourCodePattern = "\\$" + Colour.values.map(_.toString).mkString("(", "|", ")")
