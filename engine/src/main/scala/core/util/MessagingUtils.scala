package core.util

import core.gameunit.{GameUnit, PlayerCharacter}

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
            case (player: PlayerCharacter, true)  => player.name + " " + player.title
            case (player: PlayerCharacter, false) => player.name
            case _                                => unit.title

    def collapseDuplicates(names: Seq[String]) =

        val namesWithCounts = LinkedHashMap[String, Int]().withDefaultValue(0)

        names foreach (namesWithCounts(_) += 1)

        (namesWithCounts map {
            case (a, 1) => a
            case (a, i) => s"[x$i] $a"
        }).toSeq
