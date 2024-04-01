package core.util

import core.Colour
import core.Colour.colourCodePattern
import core.gameunit.{GameUnit, PlayerCharacter}

import scala.annotation.tailrec
import scala.collection.mutable.LinkedHashMap

object MessagingUtils:

    def joinOrElse(strings: Iterable[String], separator: String, default: String) =
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

    def groupedIgnoringColourCodes(message: String, size: Int): Iterator[String] =

        // TODO: account for msg containing \n
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

        reduce(message.split(' ').filterNot(_.isBlank).toList, "", 0).linesIterator

    def substituteColours(msg: String, mapper: Colour => String) = {
        val colourCodeRegex = colourCodePattern.r
        colourCodeRegex.replaceAllIn(msg, _ match
            case colourCodeRegex(colourCode) =>
                mapper(Colour.valueOf(colourCode)))
    }
