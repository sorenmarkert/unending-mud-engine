package core.gameunit

import core.gameunit.Direction.*

object Builder:

    def room(id: String)(init: Room ?=> Unit) =
        given r: Room = Room(id)

        init
        r

    def name(n: String)(using u: Containable[?]) =
        u.name = n

    def title(t: String)(using u: GameUnit) =
        u.title = t

    def description(d: String)(using u: GameUnit) =
        u.description = d


    def north(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.addLink(North, globalState.rooms(d), distance)

    def south(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.addLink(South, globalState.rooms(d), distance)

    def east(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.addLink(East, globalState.rooms(d), distance)

    def west(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.addLink(West, globalState.rooms(d), distance)

    def up(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.addLink(Up, globalState.rooms(d), distance)

    def down(d: String, distance: Int = 1)(using r: Room, globalState: GlobalState) =
        r.addLink(Down, globalState.rooms(d), distance)


    // TODO: create Item with ""s and set them after? make more sensible
    def item(name: String, title: String = "", description: String = "")(init: Item ?=> Unit)(using u: GameUnit) =
        given i: Item = u.createItem(name, title, description)

        init

    def itemSlot(s: ItemSlot)(using i: Item) =
        i.itemSlot = Option(s)
        i.outside match
            case npc: NonPlayerCharacter => npc equip i
            case _                       =>

    def npc(name: String, title: String = "", description: String = "")(init: NonPlayerCharacter ?=> Unit)(using r: Room) =
        given i: NonPlayerCharacter = r.createNonPlayerCharacter(name, title, description)

        init
