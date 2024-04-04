package core

import core.MiniMap.*
import core.gameunit.*
import core.gameunit.Gender.*
import core.util.MessagingUtils.{groupedIgnoringColourCodes, substituteColours, unitDisplay}

import scala.util.matching.Regex

class MessageSender:

    val nounRegex: Regex = "\\$([1-3])([aemsnNpt])".r
    val verbRegex: Regex = "\\$\\[(\\w+)\\|(\\w+)]".r

    val textWidth = 50

    def sendMessageToRoomOf(character: Mobile, message: String): Seq[PlayerCharacter] =
        character.outside.mobiles.flatMap(c => sendMessageToCharacter(c, message))

    def sendMessageToBystandersOf(character: Mobile, message: String): Seq[PlayerCharacter] =
        val characters = character.outside.mobiles.filterNot(_ == character)
        characters.flatMap(c => sendMessageToCharacter(c, message))

    def sendMessageToCharacters(characters: Seq[Mobile], message: String): Seq[PlayerCharacter] =
        characters.flatMap(c => sendMessageToCharacter(c, message))

    def sendMessageToCharacter(character: Mobile, message: String): Seq[PlayerCharacter] =

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
                Seq(pc)
            case _ => Seq() // TODO: send to controlling admin
    end sendMessageToCharacter

    def sendAllEnqueuedMessages(character: Mobile, addMiniMap: Boolean = false, addPrompt: Boolean = true): Unit =

        val prompt = "(12/20) fake-prompt (12/20)"
        val promptLines = if addPrompt then groupedIgnoringColourCodes(prompt, textWidth).toSeq else Seq()

        val mapLines = (addMiniMap, character.outside) match
            case (true, room: Room) => colourMiniMap(frameMiniMap(miniMap(room, 3)))
            case _ => Seq()

        character match
            case pc: PlayerCharacter =>
                def subColours = substituteColours(_, pc.connection.substituteColourCodes)
                pc.connection.sendEnqueuedMessages(
                    promptLines.map(subColours),
                    mapLines.map(subColours))
            case _ => // TODO: send to controlling admin
    end sendAllEnqueuedMessages

    def act(message: String, visibility: ActVisibility,
            actor: Option[Mobile], medium: Option[Findable] = None, target: Option[Findable] = None,
            text: Option[String] = None): Seq[PlayerCharacter] =

        def isSame(unitA: GameUnit, unitB: Option[Findable]) =
            unitB.contains(unitA)

        val charactersInRoom = actor.toSeq.flatMap(_.outside.mobiles)
        val bystanders = charactersInRoom filterNot (isSame(_, actor)) filterNot (isSame(_, target))

        def format3rdPersonUnit(unit: Findable, formatterLetter: String) =
            formatterLetter match // TODO: visibility
                case "a" => if Set('a', 'e', 'i', 'o').contains(unit.name.head) then "an" else "a"
                case "e" => formatGender(unit, _.subject)
                case "m" => formatGender(unit, _.obJect)
                case "s" => formatGender(unit, _.possessive)
                case "n" => unitDisplay(unit, includePlayerTitle = false)
                case "N" => unit.name
                case "p" => "unit.position" // TODO: positions
                case "t" => text.getOrElse("null")

        def formatGender(unit: Findable, genderToNoun: Gender => String) =
            unit match
                case character: Mobile => genderToNoun(character.gender)
                case _ => genderToNoun(GenderNeutral)

        def format2ndPersonUnit(unit: Findable, formatterLetter: String) =
            formatterLetter match // TODO: visibility
                case "a" => ""
                case "s" => "your"
                case "e" | "m" | "n" | "N" => "you"
                case "p" => "unit.position" // TODO: positions
                case "t" => text.getOrElse("null")

        val nounUnits = Map("1" -> actor, "2" -> medium, "3" -> target)

        def replaceFormatters(msg: String, indexOf2ndPerson: String) = {
            val messageWithNouns = nounRegex.replaceAllIn(msg, {
                case nounRegex(unitIndex, formatterLetter) =>
                    val formatUnit = if unitIndex == indexOf2ndPerson then format2ndPersonUnit else format3rdPersonUnit
                    nounUnits(unitIndex)
                        .map(formatUnit(_, formatterLetter))
                        .getOrElse("[None]")
            })
            val messageWithNounsAndVerbs = verbRegex.replaceAllIn(messageWithNouns, {
                case verbRegex(secondPerson, thirdPerson) =>
                    if indexOf2ndPerson == "1" then secondPerson else thirdPerson
            })
            messageWithNounsAndVerbs
        }

        val actorRecipient = actor.toSeq.flatMap(sendMessageToCharacter(_, replaceFormatters(message, "1")))
        val bystanderRecipients = bystanders.flatMap(sendMessageToCharacter(_, replaceFormatters(message, "")))
        val targetRecipient = (target match {
            case Some(m: Mobile) => Seq(m)
            case _ => Seq()
        }).flatMap(sendMessageToCharacter(_, replaceFormatters(message, "3")))

        actorRecipient ++ bystanderRecipients ++ targetRecipient
    end act


object MessageSender:
    given MessageSender = new MessageSender


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
    val colourCodePattern: String = "\\$" + Colour.values.map(_.toString).mkString("(", "|", ")")
