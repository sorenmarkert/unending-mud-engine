package core

import core.PathFinding.findPath
import core.gameunit.Direction.*
import core.gameunit.{Direction, Room}
import core.state.GlobalState
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}

class PathFindingTest extends AnyWordSpec with GivenWhenThen with Matchers with BeforeAndAfterEach:

    given globalState: GlobalState = GlobalState()

    override def beforeEach(): Unit =
        globalState.clear()

    "findPath" should {

        "Response, that you're at the destination" in {

            Given("A room")
            val room = Room("room")

            When("Calling findPath from and to the same room")
            val result = findPath(room, room)

            Then("The response says so")
            result shouldBe Left("You're already there.")
        }

        "Respond, that there's no path" in {

            Given("Two unconnected rooms")
            val fromRoom = Room("fromRoom")
            val toRoom = Room("toRoom")

            When("Calling findPath to the unconnected room")
            val result = findPath(fromRoom, toRoom)

            Then("The response says so")
            result shouldBe Left("No path found to the destination.")
        }

        "Find a path to a neighbouring room" in {

            Given("Two connected rooms")
            val fromRoom = Room("fromRoom")
            val toRoom = Room("toRoom")
                .northTo(fromRoom)

            When("Calling findPath to the connected room")
            val result = findPath(fromRoom, toRoom)

            Then("The path is returned")
            result shouldBe Right(Seq(South))
        }

        "Find the shortest path to a room" in {

            Given("Four connected rooms")
            val fromRoom = Room("fromRoom")
            val toRoom = Room("toRoom")
                .northTo(fromRoom)
            val roomWest = Room("roomWest")
                .eastTo(fromRoom, 3)
            Room("roomSouthWest")
                .northTo(roomWest)
                .eastTo(toRoom)

            When("Calling findPath to a neighbouring room")
            val result = findPath(fromRoom, toRoom)

            Then("The shortest path is returned")
            result shouldBe Right(Seq(South))
        }

        "Find the path to a room" in {

            Given("Some connected rooms")
            val roomCenter = Room("roomCenter")
            val roomNorth = Room("roomNorth")
                .southTo(roomCenter)
            val roomEast = Room("roomEast")
                .westTo(roomCenter, 2)
            val roomWest = Room("roomWest")
                .eastTo(roomCenter, 3)
            val roomSouth = Room("roomSouth")
                .northTo(roomCenter)

            val roomNorthEast = Room("roomNorthEast")
                .westTo(roomNorth)
            Room("roomEastNorth")
                .southTo(roomEast)
            Room("roomSouthWest")
                .eastTo(roomSouth)
            val roomWestSouth = Room("roomWestSouth")
                .northTo(roomWest)

            When("Calling findPath to the unconnected room")
            val result = findPath(roomWestSouth, roomNorthEast)

            Then("The response says so")
            result shouldBe Right(Seq(North, East, North, East))
        }
    }