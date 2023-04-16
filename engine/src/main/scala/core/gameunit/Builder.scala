package core.gameunit

import core.GlobalState
import core.gameunit.Direction.South
import core.gameunit.GameUnit.{createItemIn, createNonPlayerCharacterIn}
import core.gameunit.ItemSlot.ItemSlotHead

object Builder:

    def room(id: String)(init: Room ?=> Unit) =
        given r: Room = Room(id)

        init

    def name(n: String)(using u: GameUnit) =
        u.name = n

    def title(t: String)(using u: GameUnit) =
        u.title = t

    def description(d: String)(using u: GameUnit) =
        u.description = d

    def north(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.northTo(globalState.rooms(d), distance)

    def south(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.southTo(globalState.rooms(d), distance)

    def east(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.eastTo(globalState.rooms(d), distance)

    def west(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.westTo(globalState.rooms(d), distance)

    def up(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.upTo(globalState.rooms(d), distance)

    def down(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.downTo(globalState.rooms(d), distance)


    def item(id: String)(init: Item ?=> Unit)(using u: GameUnit) =
        given i: Item = createItemIn(u, id)

        init

    def itemSlot(s: ItemSlot)(using i: Item) =
        i.itemSlot = Option(s)
        i.outside match
            case Some(npc: NonPlayerCharacter) => npc equip i
            case _                             =>

    def npc(id: String)(init: NonPlayerCharacter ?=> Unit)(using u: GameUnit) =
        given i: NonPlayerCharacter = createNonPlayerCharacterIn(u, id)

        init
