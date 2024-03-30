package core

import core.ActRecipient.*
import core.Colour.colourCodePattern
import core.MiniMap.*
import core.connection.Output
import core.gameunit.*
import core.gameunit.Gender.*
import core.util.MessagingUtils.unitDisplay

class MessageSender:

    def sendMessage(character: Mobile, message: String, addMap: Boolean = false, addPrompt: Boolean = true) =

        // TODO: don't send prompt until all messages have been sent
        val textWidth = 50

        def groupedIgnoringColourCodes(msg: String) =
            msg.split(" ").filterNot(_.isBlank).reduceLeft(
                (accumulated, toAdd) => {
                    val toAddWithoutColourCodes = toAdd.replaceAll(colourCodePattern, "")
                    if accumulated.length + toAddWithoutColourCodes.length > textWidth then
                        accumulated + "\n" + toAdd
                    else
                        accumulated + " " +  toAdd
                }).linesIterator

        val formattedMessageLines =
            message
                .linesIterator
                .flatMap(groupedIgnoringColourCodes)

        val prompt = "(12/20) fake-prompt (12/20)"
        val promptLines = if addPrompt then (prompt grouped textWidth).toList else Seq()

        val mapLines = (addMap, character.outside) match {
            case (true, room: Room) => colourMiniMap(frameMiniMap(miniMap(room, 3)))
            case _ => Seq()
        }

        def substituteColours(msg: String, mapper: Colour => String) = {
            val colourCodeRegex = colourCodePattern.r
            colourCodeRegex.replaceAllIn(msg, _ match
                case colourCodeRegex(colourCode) => mapper(Colour.valueOf(colourCode)))
        }
                    
        character match
            case pc: PlayerCharacter => pc.connection.send(
                Output(
                    formattedMessageLines.toSeq.map(substituteColours(_, pc.connection.substituteColourCodes)),
                    promptLines.map(substituteColours(_, pc.connection.substituteColourCodes)),
                    mapLines.map(substituteColours(_, pc.connection.substituteColourCodes))))
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

        recipients foreach (sendMessage(_, replaceUnits(message).capitalize))
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
