package core

import core.MiniMap.miniMap
import core.gameunit.Direction.*
import core.gameunit.{Exit, Room}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

class MiniMapTest extends AnyWordSpec with GivenWhenThen with Matchers with BeforeAndAfterEach {

    given globalState: GlobalState = new GlobalState()

    override def beforeEach() =
        globalState.clear()

    "The mini map" should {

        "Map the zero range map" in {

            Given("A room with an exit")
            val roomCenter: Room = Room("roomCenter")
            val roomWest  : Room = Room("roomWest")
                .eastTo(roomCenter)

            When("Mapping the area")
            val result = miniMap(roomCenter, 0)

            Then("The resulting map is correct")
            result shouldBe List("X")
        }

        "Map a room with no exits in range 1" in {

            Given("A room with no exits")
            val roomCenter: Room = Room("roomCenter")

            When("Mapping the area")
            val result = miniMap(roomCenter, 1)

            Then("The resulting map is correct")
            result shouldBe List(
                "       ",
                "       ",
                "   X   ",
                "       ",
                "       ")
        }

        "Map a partial range 1 map" in {

            Given("A room with 2 exits")
            val roomCenter: Room = Room("roomCenter")
            val roomWest  : Room = Room("roomWest")
                .eastTo(roomCenter)
            val roomSouth : Room = Room("roomSouth")
                .northTo(roomCenter)

            When("Mapping the area")
            val result = miniMap(roomCenter, 1)

            Then("The resulting map is correct")
            result shouldBe List(
                "       ",
                "       ",
                "#--X   ",
                "   |   ",
                "   #   ")
        }

        "Map a full range 1 map" in {

            Given("A room with 4 exits, where one has a further connection")
            val roomCenter   : Room = Room("roomCenter")
            val roomNorth    : Room = Room("roomNorth")
                .southTo(roomCenter)
            val roomEast     : Room = Room("roomEast")
                .westTo(roomCenter)
            val roomWest     : Room = Room("roomWest")
                .eastTo(roomCenter)
            val roomSouth    : Room = Room("roomSouth")
                .northTo(roomCenter)
            val roomSouthWest: Room = Room("roomSouthWest")
                .eastTo(roomCenter) // Should not be include

            When("Mapping the area")
            val result = miniMap(roomCenter, 1)

            Then("The resulting map is correct")
            result shouldBe List(
                "   #   ",
                "   |   ",
                "#--X--#",
                "   |   ",
                "   #   ")
        }

        "Map a range 2 map" in {

            Given("An area of rooms, one of which is at range 3")
            val roomCenter: Room = Room("roomCenter")
            val roomNorth : Room = Room("roomNorth")
                .southTo(roomCenter)
            val roomEast  : Room = Room("roomEast")
                .westTo(roomCenter)
            val roomWest  : Room = Room("roomWest")
                .eastTo(roomCenter)
            val roomSouth : Room = Room("roomSouth")
                .northTo(roomCenter)

            val roomNorthNorth: Room = Room("roomNorthNorth")
                .southTo(roomNorth)
            val roomNorthEast : Room = Room("roomNorthEast")
                .westTo(roomNorth)
            val roomSouthSouth: Room = Room("roomSouthSouth")
                .northTo(roomSouth)
            val roomEastSouth : Room = Room("roomEastSouth")
                .northTo(roomEast)

            val roomNorthNorthEast: Room = Room("roomNorthNorthEast") // Should not b"roomNorthNorthEaste
                .westTo(roomNorthNorth)

            When("Mapping the area")
            val result = miniMap(roomCenter, 2)

            Then("The resulting map is correct")
            result shouldBe List(
                "      #      ",
                "      |      ",
                "      #--#   ",
                "      |      ",
                "   #--X--#   ",
                "      |  |   ",
                "      #  #   ",
                "      |      ",
                "      #      ")
        }

        "Map a range 2 map correctly distancing rooms" in {

            Given("An area of rooms, some of which have distance > 1")
            val roomCenter: Room = Room("roomCenter")
            val roomNorth : Room = Room("roomNorth")
                .southTo(roomCenter)
            val roomEast  : Room = Room("roomEast")
                .westTo(roomCenter, 2)
            val roomWest  : Room = Room("roomWest")
                .eastTo(roomCenter, 3)
            val roomSouth : Room = Room("roomSouth")
                .northTo(roomCenter)

            val roomNorthEast: Room = Room("roomNorthEast")
                .westTo(roomNorth)
            val roomEastNorth: Room = Room("roomEastNorth")
                .southTo(roomEast)
            val roomSouthWest: Room = Room("roomSouthWest")
                .eastTo(roomSouth)
            val roomWestSouth: Room = Room("roomWestSouth")
                .northTo(roomWest)

            When("Mapping the area")
            val result = miniMap(roomCenter, 2)

            Then("The resulting map is correct")
            result shouldBe List(
                "                   ",
                "                   ",
                "         #--#  #   ",
                "         |     |   ",
                "#--------X-----#   ",
                "|        |         ",
                "#     #--#         ",
                "                   ",
                "                   ")
        }
    }

}
