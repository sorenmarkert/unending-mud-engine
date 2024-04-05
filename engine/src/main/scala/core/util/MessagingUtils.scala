package core.util

import core.Colour
import core.Colour.colourCodePattern
import core.gameunit.{GameUnit, ItemSlot, Mobile, PlayerCharacter}

import scala.annotation.tailrec
import scala.collection.mutable.LinkedHashMap
import scala.util.matching.Regex

object MessagingUtils:

    def joinOrElse(strings: Iterable[String], separator: String = "\n", default: String = "Nothing.") =
        if strings.isEmpty then
            default
        else
            strings mkString separator

    // TODO: move onto units
    def unitDisplay(unit: GameUnit, includePlayerTitle: Boolean = true) =
        (unit, includePlayerTitle) match
            case (player: PlayerCharacter, true) => player.name + " " + player.title
            case (player: PlayerCharacter, false) => player.name
            case _ => unit.title

    def collapseDuplicates(names: Seq[String]) =

        val namesWithCounts = LinkedHashMap[String, Int]().withDefaultValue(0)

        names foreach (namesWithCounts(_) += 1)

        (namesWithCounts map {
            case (a, 1) => a
            case (a, i) => s"[x$i] $a"
        }).toSeq

    def getEquipmentDisplay(character: Mobile) =
        val slotMaxLength = ItemSlot.values.map(_.display.length).max
        ItemSlot.values.map { slot =>
            val slotDisplay = s"${slot.display}:$$s" + (slotMaxLength + 3 - slot.display.length)
            val itemDisplay = character.equippedAt(slot).map(_.title).getOrElse("-nothing-")
            s"$$Reset$slotDisplay $itemDisplay"
        }

    def groupedIgnoringColourCodes(message: String, size: Int): Iterator[String] =

        // TODO: account for msg containing \n
        // TODO: add formatter code for spaces
        @tailrec
        def reduce(words: List[String], accumulated: String, accumulatedLength: Int): String =
            words match
                case toAdd :: restWords =>
                    val lengthToAddWithoutColourCodes = toAdd.replaceAll(colourCodePattern, "").length
                    val (newAccu, newAccuLength) =
                        if accumulatedLength + lengthToAddWithoutColourCodes > size then
                            (accumulated + "\n" + toAdd, lengthToAddWithoutColourCodes)
                        else if accumulated == "" then
                            (toAdd, lengthToAddWithoutColourCodes)
                        else
                            (accumulated + " " + toAdd, accumulatedLength + 1 + lengthToAddWithoutColourCodes)
                    reduce(restWords, newAccu, newAccuLength)
                case _ => accumulated

        reduce(message.split(' ').filterNot(_.isBlank).map(substituteSpaces).toList, "", 0).linesIterator

    def substituteColours(message: String, mapper: Colour => String) =
        val colourCodeRegex = colourCodePattern.r
        colourCodeRegex.replaceAllIn(message, {
            case colourCodeRegex(colourCode) => mapper(Colour.valueOf(colourCode))
        })

    private def substituteSpaces(message: String) =
        val spacesRegex: Regex = "\\$s(\\d+)".r
        spacesRegex.replaceAllIn(message, {
            case spacesRegex(colourCode) => "".padTo(colourCode.toInt, ' ')
        })
