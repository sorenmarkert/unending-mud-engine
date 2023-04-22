package core

import akka.event.slf4j.{Logger, SLF4JLogging}
import core.gameunit.Builder.*
import core.gameunit.Direction.*
import core.gameunit.ItemSlot.*

object ZoneData extends SLF4JLogging:

    log.info("Loading Zone")

    room("roomCenter") {

        title("The Room in the Center")

        npc("driver") {
            name("driver")
            title("the driver")
            description("She's a short, plump woman wearing a light frown as if it was her most prized possession.")
        }

        npc("boy") {
            name("boy")
            title("a boy")
            description("He's a little boy, probably around the age of 10.")

            item("tanto") {
                name("black and gold plastic tanto")
                title("a black and gold plastic tanto")
                description("It's a very cheap looking tanto made of plastic. That design is quite flashy, though, provided you don't look too closely. It has a grey hilt wrapped in some black cloth ribbon.")
                itemSlot(ItemSlotOffHand)
            }
        }

        item("hat") {
            name("white cowboy hat")
            title("a white cowboy hat")
            description("It's a white cowboy hat made of some light, paper like material. It looks like it was cheap, yet it still looks incredibly cool at a distance.")
            itemSlot(ItemSlotHead)
        }

        item("hat") {
            name("black top hat")
            title("a black top hat")
            description("It's a very fancy hat, about one hand tall with a flat top and a narrow brim. And it's completely black.")
            itemSlot(ItemSlotHead)
        }

        item("bag") {
            name("bag")
            title("a bag")
            description("It's a small, red Eastpak.")

            item("book") {
                name("small book")
                title("a small book")
                description("It's a small book, bound in leather.")
            }
        }
    }

    room("roomNorth") {
        title("The Room to the North")
        south("roomCenter")
    }

    room("roomSouth") {
        title("The Room to the South")
        north("roomCenter")
    }

    room("roomEast") {
        title("The Room to the East")
        west("roomCenter")
    }

    room("roomWest") {
        title("The Room to the West")
        east("roomCenter")
    }

    room("roomNorthNorth") {
        title("The Room to the NorthNorth")
        south("roomNorth")
    }

    room("roomNorthEast") {
        title("The Room to the NorthEast")
        west("roomNorth", 2)
    }

    room("roomSouthSouth") {
        title("The Room to the SouthSouth")
        north("roomSouth")
    }

    room("roomEastSouth") {
        title("The Room to the EastSouth")
        north("roomEast")
    }

    room("roomNorthNorthEast") {
        title("The Room to the NorthNorthEast")
        west("roomNorthNorth")
    }

    room("roomNorthEastSouth") {
        title("The Room to the NorthEastSouth")
        north("roomNorthEast")
    }

    room("roomNorthNorthEastEast") {
        title("The Room to the NorthNorthEastEast")
        west("roomNorthNorthEast")
    }
