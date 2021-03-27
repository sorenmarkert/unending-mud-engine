package core

import core.Direction.{North, South, West}
import core.GameUnit.{createItemIn, createNonPlayerCharacterIn}

object ZoneData {

    val north = Room()
    north.id = "id1"
    north.name = "The North Room"
    north.title = "The North Room"
    north.description = "It's a room. There's nothing in it. Not even a door."

    val south = Room()
    south.id = "id2"
    south.name = "The South Room"
    south.title = "The South Room"
    south.description = "It's a room. There's nothing in it. Not even a door. You can head west from here, but BEWARE, you can NOT go back."

    val void = Room()
    void.id = "void1"
    void.name = "The Void"
    void.title = "The Void"
    void.description = "It's a room. There's nothing in it. Not even a door."

    north.exits += (South -> south)
    south.exits += (North -> north)
    south.exits += (West -> void)

    val book = createItemIn(north)
    book.id = "book1"
    book.name = "book"
    book.title = "a book"
    book.description = "It's a small book, bound in leather."

    val hat = createItemIn(north)
    hat.id = "hat1"
    hat.name = "hat"
    hat.title = "a white cowboy hat"
    hat.description = "It's a white cowboy hat made of some light, paper like material. It looks like it was cheap, yet it still looks incredibly cool at a distance."
    hat.itemSlot = Some(ItemSlotHead)

    val bag = createItemIn(north)
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

    val driver = createNonPlayerCharacterIn(north)
    driver.id = "driver1"
    driver.name = "driver"
    driver.title = "The driver"
    driver.description = "She's a short, plump woman wearing a light frown as if it was her most prized possession."

    val boy = createNonPlayerCharacterIn(north)
    boy.id = "boy1"
    boy.name = "boy"
    boy.title = "A boy"
    boy.description = "He's a little boy, probably around the age of 10."
}
