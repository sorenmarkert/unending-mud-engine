package core

import core.gameunit.Direction.{East, North, South, West}
import core.gameunit.{Exit, Room}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

class MiniMapTest extends AnyWordSpec with GivenWhenThen with Matchers with BeforeAndAfterEach {

    "The mini map" should {

        "Map the zero range map" in {

            Given("A room with an exit")
            val roomCenter: Room = Room("roomCenter")
                .withTitle("Some Title")
            val roomWest  : Room = Room("roomWest")
                .withTitle("Some Title")
                .eastTo(roomCenter)

            When("Mapping the area")
            val result = MiniMap.miniMap(roomCenter, 0)

            Then("The resulting map is correct")
            result.mkString("\n", "\n", "\n") shouldBe "\nX\n"
        }

        "Map a room with no exits in range 1" in {

            Given("A room with no exits")
            val roomCenter: Room = Room("roomCenter")
                .withTitle("Some Title")

            When("Mapping the area")
            val result = MiniMap.miniMap(roomCenter, 1)

            Then("The resulting map is correct")
            result.mkString("\n", "\n", "\n") shouldBe List(
                "       ",
                "       ",
                "   X   ",
                "       ",
                "       ",
                ).mkString("\n", "\n", "\n")
        }

        "Map a partial range 1 map" in {

            Given("A room with 2 exits")
            val roomCenter: Room = Room("roomCenter")
                .withTitle("Some Title")
            val roomWest  : Room = Room("roomWest")
                .withTitle("Some Title")
                .eastTo(roomCenter)
            val roomSouth : Room = Room("roomSouth")
                .withTitle("Some Title")
                .northTo(roomCenter)

            When("Mapping the area")
            val result = MiniMap.miniMap(roomCenter, 1)

            Then("The resulting map is correct")
            result.mkString("\n", "\n", "\n") shouldBe List(
                "       ",
                "       ",
                "#--X   ",
                "   |   ",
                "   #   ",
                ).mkString("\n", "\n", "\n")
        }

        "Map a full range 1 map" in {

            Given("A room with 4 exits, where one has a further connection")
            val roomCenter   : Room = Room("roomCenter")
                .withTitle("Some Title")
            val roomNorth    : Room = Room("roomNorth")
                .withTitle("Some Title")
                .southTo(roomCenter)
            val roomEast     : Room = Room("roomEast")
                .withTitle("Some Title")
                .westTo(roomCenter)
            val roomWest     : Room = Room("roomWest")
                .withTitle("Some Title")
                .eastTo(roomCenter)
            val roomSouth    : Room = Room("roomSouth")
                .withTitle("Some Title")
                .northTo(roomCenter)
            val roomSouthWest: Room = Room("roomSouthWest")
                .withTitle("Some Title")
                .eastTo(roomCenter) // Should not be include

            When("Mapping the area")
            val result = MiniMap.miniMap(roomCenter, 1)

            Then("The resulting map is correct")
            result.mkString("\n", "\n", "\n") shouldBe List(
                "   #   ",
                "   |   ",
                "#--X--#",
                "   |   ",
                "   #   ",
                ).mkString("\n", "\n", "\n")
        }

        "Map a range 2 map" in {

            Given("An area of rooms, one of which is at range 3")
            val roomCenter: Room = Room("roomCenter")
                .withTitle("Some Title")
            val roomNorth : Room = Room("roomNorth")
                .withTitle("Some Title")
                .southTo(roomCenter)
            val roomEast  : Room = Room("roomEast")
                .withTitle("Some Title")
                .westTo(roomCenter)
            val roomWest  : Room = Room("roomWest")
                .withTitle("Some Title")
                .eastTo(roomCenter)
            val roomSouth : Room = Room("roomSouth")
                .withTitle("Some Title")
                .northTo(roomCenter)

            val roomNorthNorth: Room = Room("roomNorthNorth")
                .withTitle("Some Title")
                .southTo(roomNorth)
            val roomNorthEast : Room = Room("roomNorthEast")
                .withTitle("Some Title")
                .westTo(roomNorth)
            val roomSouthSouth: Room = Room("roomSouthSouth")
                .withTitle("Some Title")
                .northTo(roomSouth)
            val roomEastSouth : Room = Room("roomEastSouth")
                .withTitle("Some Title")
                .northTo(roomEast)

            val roomNorthNorthEast: Room = Room("roomNorthNorthEast") // Should not b"roomNorthNorthEaste
                // .included withTitle("Some Title")
                .westTo(roomNorthNorth)

            When("Mapping the area")
            val result = MiniMap.miniMap(roomCenter, 2)

            Then("The resulting map is correct")
            result.mkString("\n", "\n", "\n") shouldBe List(
                "      #      ",
                "      |      ",
                "      #--#   ",
                "      |      ",
                "   #--X--#   ",
                "      |  |   ",
                "      #  #   ",
                "      |      ",
                "      #      ",
                ).mkString("\n", "\n", "\n")
        }

        "Map a range 2 map correctly distancing rooms" in {

            Given("An area of rooms, some of which have distance > 1")
            val roomCenter: Room = Room("roomCenter")
                .withTitle("Some Title")
            val roomNorth : Room = Room("roomNorth")
                .withTitle("Some Title")
                .southTo(roomCenter)
            val roomEast  : Room = Room("roomEast")
                .withTitle("Some Title")
                .westTo(roomCenter, 2)
            val roomWest  : Room = Room("roomWest")
                .withTitle("Some Title")
                .eastTo(roomCenter, 3)
            val roomSouth : Room = Room("roomSouth")
                .withTitle("Some Title")
                .northTo(roomCenter)

            val roomNorthEast: Room = Room("roomNorthEast")
                .withTitle("Some Title")
                .westTo(roomNorth)
            val roomEastNorth: Room = Room("roomEastNorth")
                .withTitle("Some Title")
                .southTo(roomEast)
            val roomSouthWest: Room = Room("roomSouthWest")
                .withTitle("Some Title")
                .eastTo(roomSouth)
            val roomWestSouth: Room = Room("roomWestSouth")
                .withTitle("Some Title")
                .northTo(roomWest)

            When("Mapping the area")
            val result = MiniMap.miniMap(roomCenter, 2)

            Then("The resulting map is correct")
            result.mkString("\n", "\n", "\n") shouldBe List(
                "                   ",
                "                   ",
                "         #--#  #   ",
                "         |     |   ",
                "#--------X-----#   ",
                "|        |         ",
                "#     #--#         ",
                "                   ",
                "                   ",
                ).mkString("\n", "\n", "\n")
        }
    }
}
