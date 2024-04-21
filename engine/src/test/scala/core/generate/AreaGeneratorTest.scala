package core.generate

import core.gameunit.*
import core.gameunit.Direction.*
import core.state.GlobalState
import org.scalatest.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random

class AreaGeneratorTest extends AnyWordSpec with GivenWhenThen with Matchers with BeforeAndAfterEach with PrivateMethodTester:

    given random: Random = Random

    given globalState: GlobalState = GlobalState()

    override def beforeEach() =
        random.setSeed(1L)
        globalState.clear()

    val testObj = AreaGenerator()

    "generate" should {

        "Generate a 2 room board and link rooms" in {

            When("Generating a 2 room board")
            val result = testObj.generate(1, 1)

            Then("A board is returned")
            result map { case (dir, room) => (dir, room.id) } shouldBe Map(
                (0, 0) -> "generated[0,0]",
                (0, -1) -> "generated[0,-1]")

            And("The rooms are linked")
            println(globalState)
            globalState.rooms("generated[0,0]").exits.keySet shouldBe Set(South)
            globalState.rooms("generated[0,0]").exits.values map (_.toRoom.id) shouldBe List("generated[0,-1]")
            globalState.rooms("generated[0,-1]").exits.keySet shouldBe Set(North)
            globalState.rooms("generated[0,-1]").exits.values map (_.toRoom.id) shouldBe List("generated[0,0]")
        }
    }

    "generateBoard" should {

        val generateBoard = PrivateMethod[Set[(Int, Int)]](Symbol("generateBoard"))

        "Generate a 1 hallway board" in {

            Given("A set of input parameters")
            val methodWithArguments = generateBoard(Set((0, 0)), (0, 0), (1, 0), 1, 1)

            When("Generating the board")
            val result = testObj invokePrivate methodWithArguments

            Then("The result is")
            result should contain(0, 0)
            result should contain atLeastOneElementOf Set((0, 1), (0, -1), (1, 0), (-1, 0))
        }

        "Generate a 5 hallway board" in {

            Given("A set of input parameters")
            val methodWithArguments = generateBoard(Set((0, 0)), (0, 0), (1, 0), 5, 5)

            When("Generating the board")
            val result = testObj invokePrivate methodWithArguments

            Then("The result is")
            result should contain allElementsOf Set((-4, 3), (-3, 4), (-6, 3), (-4, 4), (-4, 1), (0, 1), (0, 4), (-6, 0), (-1, 4), (-5, -1), (0, 0), (-4, 0), (-6, -1), (0, 2), (-6, 1), (-4, -1), (-4, 2), (-6, 2), (0, 3), (-2, 4))
        }
    }

    "getAdjacentRooms" should {

        val getAdjacentRooms = PrivateMethod[Map[Room, Direction]](Symbol("getAdjacentRooms"))

        "Return adjacent rooms" in {

            Given("A board with rooms")
            val boardWithRooms = Map(
                (0, 0) -> Room("centerRoom"),
                (0, 1) -> Room("adjacentRoomNorth"),
                (0, -1) -> Room("adjacentRoomSouth"),
                (1, 0) -> Room("adjacentRoomEast"),
                (-1, 0) -> Room("adjacentRoomWest"),
                (-2, 0) -> Room("nonAdjacentRoomWest"),
                (-1, 1) -> Room("nonAdjacentRoomNorthWest"))

            When("Getting the rooms adjacent to the center room")
            val result = testObj invokePrivate getAdjacentRooms((0, 0), boardWithRooms)

            Then("The adjacent rooms are returned")
            result map { case (room, direction) => (room.id, direction) } shouldBe Map(
                "adjacentRoomNorth" -> North,
                "adjacentRoomSouth" -> South,
                "adjacentRoomEast" -> East,
                "adjacentRoomWest" -> West)
        }
    }
