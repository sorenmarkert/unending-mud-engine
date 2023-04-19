package core

import core.MiniMap.miniMap
import core.gameunit.Direction.*
import core.gameunit.*
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
            Room("roomWest")
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
            Room("roomWest")
                .eastTo(roomCenter)
            Room("roomSouth")
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
            Room("roomNorth")
                .southTo(roomCenter)
            Room("roomEast")
                .westTo(roomCenter)
            Room("roomWest")
                .eastTo(roomCenter)
            Room("roomSouth")
                .northTo(roomCenter)
            Room("roomSouthWest")
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
            Room("roomWest")
                .eastTo(roomCenter)
            val roomSouth : Room = Room("roomSouth")
                .northTo(roomCenter)

            val roomNorthNorth: Room = Room("roomNorthNorth")
                .southTo(roomNorth)
            Room("roomNorthEast")
                .westTo(roomNorth)
            Room("roomSouthSouth")
                .northTo(roomSouth)
            Room("roomEastSouth")
                .northTo(roomEast)

            Room("roomNorthNorthEast") // Should not b"roomNorthNorthEaste
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

            Room("roomNorthEast")
                .westTo(roomNorth)
            Room("roomEastNorth")
                .southTo(roomEast)
            Room("roomSouthWest")
                .eastTo(roomSouth)
            Room("roomWestSouth")
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
