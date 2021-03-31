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
    val roomNorthNorthEastEast = Room()
    roomNorthNorthEastEast.title = "The Room to the NorthNorthEastEast"

    roomCenter.exits += (North -> roomNorth)
    roomCenter.exits += (South -> roomSouth)
    roomCenter.exits += (East -> roomEast)
    roomCenter.exits += (West -> roomWest)

    roomNorth.exits += (North -> roomNorthNorth)
    roomNorth.exits += (South -> roomCenter)
    roomNorth.exits += (East -> roomNorthEast)

    roomSouth.exits += (North -> roomCenter)
    roomSouth.exits += (South -> roomSouthSouth)

    roomEast.exits += (South -> roomEastSouth)
    roomEast.exits += (West -> roomCenter)

    roomWest.exits += (East -> roomCenter)

    roomNorthNorth.exits += (East -> roomNorthNorthEast)
    roomNorthNorth.exits += (South -> roomNorth)

    roomNorthEast.exits += (West -> roomNorth)

    roomSouthSouth.exits += (North -> roomSouth)

    roomEastSouth.exits += (North -> roomEast)

    roomNorthNorthEast.exits += (East -> roomNorthNorthEastEast)
    roomNorthNorthEast.exits += (West -> roomNorthNorth)

    roomNorthNorthEastEast.exits += (West -> roomNorthNorthEast)

    val book = createItemIn(roomCenter)
    book.id = "book1"
    book.name = "book"
    book.title = "a book"
    book.description = "It's a small book, bound in leather."

    val hat = createItemIn(roomCenter)
    hat.id = "hat1"
    hat.name = "hat"
    hat.title = "a white cowboy hat"
    hat.description = "It's a white cowboy hat made of some light, paper like material. It looks like it was cheap, yet it still looks incredibly cool at a distance."
    hat.itemSlot = Some(ItemSlotHead)

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
    driver.title = "The driver"
    driver.description = "She's a short, plump woman wearing a light frown as if it was her most prized possession."

    val boy = createNonPlayerCharacterIn(roomCenter)
    boy.id = "boy1"
    boy.name = "boy"
    boy.title = "A boy"
    boy.description = "He's a little boy, probably around the age of 10."
}
