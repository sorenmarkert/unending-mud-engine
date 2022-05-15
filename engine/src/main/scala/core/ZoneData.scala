package core

import core.ZoneData.roomSouth
import core.gameunit.Direction.*
import core.gameunit.GameUnit.{createItemIn, createNonPlayerCharacterIn}
import core.gameunit.ItemSlot.{ItemSlotHead, ItemSlotOffHand}
import core.gameunit.*

object ZoneData {

    val roomCenter: Room = Room("The Room in the Center")

    val roomNorth: Room = Room("The Room to the North")
        .southTo(roomCenter)

    val roomSouth: Room = Room("The Room to the South")
        .northTo(roomCenter)

    val roomEast: Room = Room("The Room to the East")
        .westTo(roomCenter)

    val roomWest: Room = Room("The Room to the West")
        .eastTo(roomCenter)

    val roomNorthNorth: Room = Room("The Room to the NorthNorth")
        .southTo(roomNorth)

    val roomNorthEast: Room = Room("The Room to the NorthEast")
        .westTo(roomNorth, 2)

    val roomSouthSouth: Room = Room("The Room to the SouthSouth")
        .northTo(roomSouth)

    val roomEastSouth: Room = Room("The Room to the EastSouth")
        .northTo(roomEast)

    val roomNorthNorthEast: Room = Room("The Room to the NorthNorthEast")
        .westTo(roomNorthNorth)

    val roomNorthEastSouth: Room = Room("The Room to the NorthEastSouth")
        .northTo(roomNorthEast)

    val roomNorthNorthEastEast: Room = Room("The Room to the NorthNorthEastEast")
        .westTo(roomNorthNorthEast)

    val book = createItemIn(roomCenter, "book1")
    book.name = "book"
    book.title = "a book"
    book.description = "It's a small book, bound in leather."

    val cowboyHat = createItemIn(roomCenter, "hat1")
    cowboyHat.name = "hat"
    cowboyHat.title = "a white cowboy hat"
    cowboyHat.description = "It's a white cowboy hat made of some light, paper like material. It looks like it was cheap, yet it still looks incredibly cool at a distance."
    cowboyHat.itemSlot = Some(ItemSlotHead)

    val topHat = createItemIn(roomCenter, "hat2")
    topHat.name = "hat"
    topHat.title = "a black top hat"
    topHat.description = "It's a very fancy hat, about one hand tall with a flat top and a narrow brim. And it's completely black."
    topHat.itemSlot = Some(ItemSlotHead)

    val bag = createItemIn(roomCenter, "bag1")
    bag.name = "bag"
    bag.title = "a bag"
    bag.description = "It's a small, red Eastpak."

    val tanto = createItemIn(bag, "tanto1")
    tanto.name = "tanto"
    tanto.title = "a black and gold plastic tanto"
    tanto.description = "It's a very cheap looking tanto made of plastic. That design is quite flashy, though, provided you don't look too closely. It has a grey hilt wrapped in some black cloth ribbon."
    tanto.itemSlot = Some(ItemSlotOffHand)

    val driver = createNonPlayerCharacterIn(roomCenter, "driver1")
    driver.name = "driver"
    driver.title = "the driver"
    driver.description = "She's a short, plump woman wearing a light frown as if it was her most prized possession."

    val boy = createNonPlayerCharacterIn(roomCenter, "boy1")
    boy.name = "boy"
    boy.title = "a boy"
    boy.description = "He's a little boy, probably around the age of 10."
}
