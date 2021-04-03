package core

import core.Direction.{East, North, South, West}
import core.GameUnit.{createItemIn, createNonPlayerCharacterIn}

object ZoneData {

    val roomCenter = Room()
    roomCenter.title = "The Room in the Center"
    val roomNorth = Room()
    roomNorth.title = "The Room to the North"
    val roomEast = Room()
    roomEast.title = "The Room to the East"
    val roomWest = Room()
    roomWest.title = "The Room to the West"
    val roomSouth = Room()
    roomSouth.title = "The Room to the South"

    val roomNorthNorth = Room()
    roomNorthNorth.title = "The Room to the NorthNorth"
    val roomNorthEast = Room()
    roomNorthEast.title = "The Room to the NorthEast"
    val roomEastSouth = Room()
    roomEastSouth.title = "The Room to the EastSouth"
    val roomSouthSouth = Room()
    roomSouthSouth.title = "The Room to the SouthSouth"

    val roomNorthNorthEast = Room()
    roomNorthNorthEast.title = "The Room to the NorthNorthEast"
    val roomNorthEastSouth = Room()
    roomNorthEastSouth.title = "The Room to the NorthEastSouth"
    val roomNorthNorthEastEast = Room()
    roomNorthNorthEastEast.title = "The Room to the NorthNorthEastEast"

    roomCenter.exits += (North -> Exit(roomNorth))
    roomCenter.exits += (South -> Exit(roomSouth))
    roomCenter.exits += (East -> Exit(roomEast))
    roomCenter.exits += (West -> Exit(roomWest))

    roomNorth.exits += (North -> Exit(roomNorthNorth))
    roomNorth.exits += (South -> Exit(roomCenter))
    roomNorth.exits += (East -> Exit(roomNorthEast, 2))

    roomSouth.exits += (North -> Exit(roomCenter))
    roomSouth.exits += (South -> Exit(roomSouthSouth))

    roomEast.exits += (South -> Exit(roomEastSouth))
    roomEast.exits += (West -> Exit(roomCenter))

    roomWest.exits += (East -> Exit(roomCenter))

    roomNorthNorth.exits += (East -> Exit(roomNorthNorthEast))
    roomNorthNorth.exits += (South -> Exit(roomNorth))

    roomNorthEast.exits += (West -> Exit(roomNorth, 2))
    roomNorthEast.exits += (South -> Exit(roomNorthEastSouth))

    roomSouthSouth.exits += (North -> Exit(roomSouth))

    roomEastSouth.exits += (North -> Exit(roomEast))

    roomNorthNorthEast.exits += (East -> Exit(roomNorthNorthEastEast))
    roomNorthNorthEast.exits += (West -> Exit(roomNorthNorth))

    roomNorthEastSouth.exits += (North -> Exit(roomNorthEast))

    roomNorthNorthEastEast.exits += (West -> Exit(roomNorthNorthEast))

    val book = createItemIn(roomCenter)
    book.id = "book1"
    book.name = "book"
    book.title = "a book"
    book.description = "It's a small book, bound in leather."

    val cowboyHat = createItemIn(roomCenter)
    cowboyHat.id = "hat1"
    cowboyHat.name = "hat"
    cowboyHat.title = "a white cowboy hat"
    cowboyHat.description = "It's a white cowboy hat made of some light, paper like material. It looks like it was cheap, yet it still looks incredibly cool at a distance."
    cowboyHat.itemSlot = Some(ItemSlotHead)

    val topHat = createItemIn(roomCenter)
    topHat.id = "hat2"
    topHat.name = "hat"
    topHat.title = "a black top hat"
    topHat.description = "It's a very fancy hat, about one hand tall with a flat top and a narrow brim. And it's completely black."
    topHat.itemSlot = Some(ItemSlotHead)

    val bag = createItemIn(roomCenter)
    bag.id = "bag1"
    bag.name = "bag"
    bag.title = "a bag"
    bag.description = "It's a small, red Eastpak."

    val tanto = createItemIn(bag)
    tanto.id = "tanto1"
    tanto.name = "tanto"
    tanto.title = "a black and gold plastic tanto"
    tanto.description = "It's a very cheap looking tanto made of plastic. That design is quite flashy, though, provided you don't look too closely. It has a grey hilt wrapped in some black cloth ribbon."
    tanto.itemSlot = Some(ItemSlotOffHand)

    val driver = createNonPlayerCharacterIn(roomCenter)
    driver.id = "driver1"
    driver.name = "driver"
    driver.title = "the driver"
    driver.description = "She's a short, plump woman wearing a light frown as if it was her most prized possession."

    val boy = createNonPlayerCharacterIn(roomCenter)
    boy.id = "boy1"
    boy.name = "boy"
    boy.title = "a boy"
    boy.description = "He's a little boy, probably around the age of 10."
}
