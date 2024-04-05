package core.gameunit

import core.state.GlobalState

enum Direction(val display: String, private val oppo: String):

    def opposite: Direction = Direction.valueOf(this.oppo)

    infix def to(toRoomId: String)(using r: Room, globalState: GlobalState): Room =
        r.addLink(this, globalState.rooms(toRoomId))

    case North extends Direction("north", "South")
    case South extends Direction("south", "North")
    case East extends Direction("east", "West")
    case West extends Direction("west", "East")
    case Up extends Direction("up", "Down")
    case Down extends Direction("down", "Up")


case class Exit(toRoom: Room, distance: Int)


enum ItemSlot(val display: String):
    case ItemSlotHead extends ItemSlot("Worn on head")
    case ItemSlotHands extends ItemSlot("Worn on hands")
    case ItemSlotChest extends ItemSlot("Worn on chest")
    case ItemSlotFeet extends ItemSlot("Worn on feet")
    case ItemSlotMainHand extends ItemSlot("Wielded in main-hand")
    case ItemSlotOffHand extends ItemSlot("Wielded in off hand")
    case ItemSlotBothHands extends ItemSlot("Two hand wielded")


enum Gender(val subject: String, val obJect: String, val possessive: String):
    case GenderMale extends Gender("he", "him", "his")
    case GenderFemale extends Gender("she", "her", "her")
    case GenderNeutral extends Gender("it", "it", "its")


enum Position:
    case Standing, Sitting, Lying
