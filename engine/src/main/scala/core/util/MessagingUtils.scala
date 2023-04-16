package core.util

import core.gameunit.{GameUnit, PlayerCharacter}

object MessagingUtils:

    def joinOrElse(strings: Iterable[String], separator: String, default: String) =
        if strings.isEmpty then
            default
        else
            strings mkString separator

    def unitDisplay(unit: GameUnit, includePlayerTitle: Boolean = true) =
        (unit, includePlayerTitle) match
            case (player: PlayerCharacter, true)  => player.name + " " + player.title
            case (player: PlayerCharacter, false) => player.name
            case _                                => unit.title
