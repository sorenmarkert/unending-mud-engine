package core

import core.Direction.{East, North, South, West}
import core.Rooms.{roomCenter, roomEast, roomEastSouth, roomNorth, roomNorthEast, roomNorthNorth, roomNorthNorthEast, roomNorthNorthEastEast, roomSouth, roomSouthSouth, roomWest}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

class MiniMapTest extends AnyWordSpec with GivenWhenThen with Matchers with BeforeAndAfterEach {

    "The mini map" should {

        "Map the zero range map" in {

            Given("A room with an exit")
            val roomCenter = Room()
            val roomWest = Room()
            roomCenter.exits += (West -> roomWest)
            roomWest.exits += (East -> roomCenter)

            When("Mapping the area")
            val result = MiniMap.miniMap(roomCenter, 0)

            Then("The resulting map is correct")
            result.mkString("\n", "\n", "\n") shouldBe "\nX\n"
        }

        "Map a room with no exits in range 1" in {

            Given("A room with no exits")
            val roomCenter = Room()

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
            val roomCenter = Room()
            val roomWest = Room()
            val roomSouth = Room()
            roomCenter.exits += (West -> roomWest)
            roomCenter.exits += (South -> roomSouth)
            roomWest.exits += (East -> roomCenter)
            roomSouth.exits += (North -> roomCenter)

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
            val roomCenter = Room()
            val roomNorth = Room()
            val roomEast = Room()
            val roomWest = Room()
            val roomSouth = Room()
            val roomSouthWest = Room()
            roomCenter.exits += (North -> roomNorth)
            roomCenter.exits += (East -> roomEast)
            roomCenter.exits += (West -> roomWest)
            roomCenter.exits += (South -> roomSouth)
            roomSouth.exits += (West -> roomSouthWest) // Should not be included
            roomNorth.exits += (South -> roomCenter)
            roomEast.exits += (West -> roomCenter)
            roomWest.exits += (East -> roomCenter)
            roomSouth.exits += (North -> roomCenter)

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
            val roomCenter = Room()
            val roomNorth = Room()
            val roomEast = Room()
            val roomWest = Room()
            val roomSouth = Room()
            roomCenter.exits += (North -> roomNorth)
            roomCenter.exits += (East -> roomEast)
            roomCenter.exits += (West -> roomWest)
            roomCenter.exits += (South -> roomSouth)
            roomNorth.exits += (South -> roomCenter)
            roomEast.exits += (West -> roomCenter)
            roomWest.exits += (East -> roomCenter)
            roomSouth.exits += (North -> roomCenter)

            val roomNorthNorth = Room()
            val roomNorthNorthEast = Room() // Should not be included
            val roomNorthEast = Room()
            val roomEastSouth = Room()
            val roomSouthSouth = Room()

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

            roomNorthNorthEast.exits += (West -> roomNorthNorth)

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
    }
}
