package core

import core.Direction.{East, North, South, West}
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
            val roomNorthNorth = Room()
            roomNorth.exits += (North -> roomNorthNorth)
            val roomNorthNorthEast = Room() // Should noe be included
            roomNorthNorth.exits += (North -> roomNorthNorthEast)
            val roomNorthEast = Room()
            roomNorth.exits += (East -> roomNorthEast)
            val roomEastSouth = Room()
            roomEast.exits += (East -> roomEastSouth)
            val roomSouthSouth = Room()
            roomSouth.exits += (South -> roomSouthSouth)

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
