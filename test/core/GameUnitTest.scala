package core

import core.GameState.global
import core.GameUnit.createItemIn
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GameUnitTest extends AnyWordSpec with GivenWhenThen with Matchers with BeforeAndAfterEach {

    override def beforeEach() = {
        global.clear()
    }

    "A newly created item" should {

        "be the only unit in an empty unit" in {

            Given("An empty room")
            val room = Room()

            When("An item is created inside it")
            val newItem = createItemIn(room)

            Then("The item is the only thing inside the room")
            room.contents should contain only newItem
            newItem.outside shouldBe Some(room)

            And("On the global list")
            global should contain only newItem
        }

        "be before other the units in a non-empty unit" in {

            Given("An room containing an item")
            val room = Room()
            val oldItem = createItemIn(room)

            When("A new item is created inside the room")
            val newItem = createItemIn(room)

            Then("The new item is the top thing inside the room")
            room.contents should contain theSameElementsInOrderAs List(newItem, oldItem)
            newItem.outside shouldBe Some(room)

            And("On the global list")
            global should contain theSameElementsInOrderAs List(newItem, oldItem)
        }
    }

    "A deleted item" should {

        "Leave the order of its siblings unchanged" in {

            Given("An room containing three items")
            val room = Room()
            val bottomItem = createItemIn(room)
            val itemToBeDeleted = createItemIn(room)
            val topItem = createItemIn(room)

            When("The middle item is removed")
            itemToBeDeleted.removeUnit

            Then("The two remaining items retain their order in the room")
            room.contents should contain theSameElementsInOrderAs List(topItem, bottomItem)

            And("On the global list")
            global should contain theSameElementsInOrderAs List(topItem, bottomItem)
        }
    }

    "A moved item" should {

        "Leave the order of its old siblings unchanged" in {

            Given("An room containing three items")
            val fromRoom = Room()
            val bottomItem = createItemIn(fromRoom)
            val itemToBeMoved = createItemIn(fromRoom)
            val topItem = createItemIn(fromRoom)

            val toRoom = Room()
            val oldItem = createItemIn(toRoom)

            When("The middle item is moved to another room")
            toRoom addUnit itemToBeMoved

            Then("The two remaining items retain their order in the old room")
            fromRoom.contents should contain theSameElementsInOrderAs List(topItem, bottomItem)

            And("The new item is the top thing inside the new room")
            toRoom.contents should contain theSameElementsInOrderAs List(itemToBeMoved, oldItem)
            itemToBeMoved.outside shouldBe Some(toRoom)

            And("The global list contains all the items")
            global should contain theSameElementsInOrderAs List(oldItem, topItem, itemToBeMoved, bottomItem)
        }
    }

    "findUnit" should {

        "Return a" in {

        }
    }
}
