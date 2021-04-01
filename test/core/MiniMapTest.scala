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
            roomCenter.exits += (West -> Exit(roomWest))
            roomWest.exits += (East -> Exit(roomCenter))

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
            roomCenter.exits += (West -> Exit(roomWest))
            roomCenter.exits += (South -> Exit(roomSouth))
            roomWest.exits += (East -> Exit(roomCenter))
            roomSouth.exits += (North -> Exit(roomCenter))

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
            roomCenter.exits += (North -> Exit(roomNorth))
            roomCenter.exits += (East -> Exit(roomEast))
            roomCenter.exits += (West -> Exit(roomWest))
            roomCenter.exits += (South -> Exit(roomSouth))
            roomSouth.exits += (West -> Exit(roomSouthWest)) // Should not be included
            roomNorth.exits += (South -> Exit(roomCenter))
            roomEast.exits += (West -> Exit(roomCenter))
            roomWest.exits += (East -> Exit(roomCenter))
            roomSouth.exits += (North -> Exit(roomCenter))

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
            roomCenter.exits += (North -> Exit(roomNorth))
            roomCenter.exits += (East -> Exit(roomEast))
            roomCenter.exits += (West -> Exit(roomWest))
            roomCenter.exits += (South -> Exit(roomSouth))
            roomNorth.exits += (South -> Exit(roomCenter))
            roomEast.exits += (West -> Exit(roomCenter))
            roomWest.exits += (East -> Exit(roomCenter))
            roomSouth.exits += (North -> Exit(roomCenter))

            val roomNorthNorth = Room()
            val roomNorthNorthEast = Room() // Should not be included
            val roomNorthEast = Room()
            val roomEastSouth = Room()
            val roomSouthSouth = Room()

            roomNorth.exits += (North -> Exit(roomNorthNorth))
            roomNorth.exits += (South -> Exit(roomCenter))
            roomNorth.exits += (East -> Exit(roomNorthEast))

            roomSouth.exits += (North -> Exit(roomCenter))
            roomSouth.exits += (South -> Exit(roomSouthSouth))

            roomEast.exits += (South -> Exit(roomEastSouth))
            roomEast.exits += (West -> Exit(roomCenter))

            roomWest.exits += (East -> Exit(roomCenter))

            roomNorthNorth.exits += (East -> Exit(roomNorthNorthEast))
            roomNorthNorth.exits += (South -> Exit(roomNorth))

            roomNorthEast.exits += (West -> Exit(roomNorth))

            roomSouthSouth.exits += (North -> Exit(roomSouth))

            roomEastSouth.exits += (North -> Exit(roomEast))

            roomNorthNorthEast.exits += (West -> Exit(roomNorthNorth))

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
            val roomCenter = Room()
            roomCenter.id = "roomCenter"
            val roomNorth = Room()
            roomNorth.id = "roomNorth"
            val roomEast = Room()
            roomEast.id = "roomEast"
            val roomWest = Room()
            roomWest.id = "roomWest"
            val roomSouth = Room()
            roomSouth.id = "roomSouth"
            roomCenter.exits += (North -> Exit(roomNorth))
            roomCenter.exits += (East -> Exit(roomEast, 2))
            roomCenter.exits += (West -> Exit(roomWest, 3))
            roomCenter.exits += (South -> Exit(roomSouth))
            roomNorth.exits += (South -> Exit(roomCenter))
            roomEast.exits += (West -> Exit(roomCenter, 2))
            roomWest.exits += (East -> Exit(roomCenter, 3))
            roomSouth.exits += (North -> Exit(roomCenter))

            val roomNorthEast = Room()
            roomNorthEast.id = "roomNorthEast"
            val roomEastNorth = Room()
            roomEastNorth.id = "roomEastNorth"
            val roomSouthWest = Room()
            roomSouthWest.id = "roomSouthWest"
            val roomWestSouth = Room()
            roomWestSouth.id = "roomWestSouth"

            roomNorth.exits += (East -> Exit(roomNorthEast))
            roomEast.exits += (North -> Exit(roomEastNorth))
            roomSouth.exits += (West -> Exit(roomSouthWest))
            roomWest.exits += (South -> Exit(roomWestSouth))

            roomNorthEast.exits += (West -> Exit(roomNorth))
            roomEastNorth.exits += (South -> Exit(roomEast))
            roomSouthWest.exits += (East -> Exit(roomSouth))
            roomWestSouth.exits += (North -> Exit(roomWest))

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
