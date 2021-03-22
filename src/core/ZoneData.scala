package core

import core.GameUnit.{createItemIn, createNonPlayerCharacter}

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
    south.description = "It's a room. There's nothing in it. Not even a door."

    north.exits += (North -> north)
    north.exits += (South -> south)
    north.exits += (West -> south)

    val book = createItemIn(north)
    book.id = "book1"
    book.name = "book"
    book.title = "A book"
    book.description = "It's a small book, bound in leather."

    val driver = createNonPlayerCharacter(north)
    driver.id = "book1"
    driver.name = "book"
    driver.title = "A book"
    driver.description = "It's a small book, bound in leather."
}
