package core

import core.GlobalState.global
import core.GameUnit.{createItemIn, createNonPlayerCharacterIn, findUnit}
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

        "Delete its contents recursively" in {

            Given("An room containing an item containing another item")
            val room = Room()
            val itemToBeDeleted = createItemIn(room)
            val containedItem = createItemIn(itemToBeDeleted)

            When("The outer item is removed")
            itemToBeDeleted.removeUnit

            Then("The the inner item is also removed")
            room.contents shouldBe empty
            containedItem.contents shouldBe empty

            And("On the global list")
            global shouldBe empty
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

    "Equipping an item" should {

        "Place the item as equipped on the character" in {

            Given("A character with an item")
            val room = Room()
            val character = createNonPlayerCharacterIn(room)
            val item = createItemIn(character)
            item.itemSlot = Some(ItemSlotHead)

            When("The character equips the item")
            val result = character equip item

            Then("It is placed in the character's equipped items")
            character.equippedItems should contain (item)

            And("The character no longer has the item in the inventory")
            character.inventory should not contain item

            And("The item is equipped")
            item.isEquipped shouldBe true

            And("No error message was returned")
            result shouldBe None
        }

        "Return an error when it's not in the character's inventory" in {

            Given("A character next to an item")
            val room = Room()
            val character = createNonPlayerCharacterIn(room)
            val item = createItemIn(room)
            item.itemSlot = Some(ItemSlotHead)

            When("The character equips the item")
            val result = character equip item

            Then("An error message is returned")
            result should contain ("You can only equip items from your inventory.")
        }

        "Return an error when the character already has an item equipped in that slot" in {

            Given("A character with two item, where one is equipped")
            val room = Room()
            val character = createNonPlayerCharacterIn(room)
            val equippedItem = createItemIn(character)
            equippedItem.itemSlot = Some(ItemSlotHead)
            character equip equippedItem
            val unequippedItem = createItemIn(character)
            unequippedItem.itemSlot = Some(ItemSlotHead)

            When("The character equips the unequipped item")
            val result = character equip unequippedItem

            Then("An error message is returned")
            result should contain ("You already have something equipped there.")
        }

        "Return an error when the item is unequippable" in {

            Given("A character with two item, where one is equipped")
            val room = Room()
            val character = createNonPlayerCharacterIn(room)
            val item = createItemIn(character)
            item.itemSlot = None

            When("The character equips the unequipped item")
            val result = character equip item

            Then("An error message is returned")
            result should contain ("This item cannot be equipped.")
        }
    }

    "Unequipping an item" should {

        "Placed it in the inventory" in {

            Given("A character with an equipped item")
            val room = Room()
            val character = createNonPlayerCharacterIn(room)
            val item = createItemIn(character)
            item.itemSlot = Some(ItemSlotHead)
            character equip item

            When("The character unequips the item")
            val result = character unequip item

            Then("It is placed in the character's inventory")
            character.inventory should contain (item)

            And("The character no longer has the item equipped")
            character.equippedItems should not contain item

            And("The item is not equipped")
            item.isEquipped shouldBe false

            And("No error message was returned")
            result shouldBe None
        }

        "Return an error when it's not equipped" in {

            Given("A character next to an item")
            val room = Room()
            val character = createNonPlayerCharacterIn(room)
            val item = createItemIn(room)

            When("The character unequips the item")
            val result = character unequip item

            Then("An error message is returned")
            result should contain ("You don't have that item equipped.")
        }
    }

    "findUnit" should {

        "Find an item next to the character" in {

            Given("A player next to 3 items")
            val room = Room()
            val player = createNonPlayerCharacterIn(room)
            val bottomItem = createItemIn(room)
            bottomItem.id = "bottomItem"
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(room)
            itemToBeFound.id = "itemToBeFound"
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(room)
            topItem.id = "topItem"
            topItem.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindNextToMe))

            Then("The correct item is returned")
            result shouldBe Some(itemToBeFound)
        }

        "Find an item in the character's inventory" in {

            Given("A player with 3 items")
            val room = Room()
            val player = createNonPlayerCharacterIn(room)
            val bottomItem = createItemIn(player)
            bottomItem.id = "bottomItem"
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(player)
            itemToBeFound.id = "itemToBeFound"
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(player)
            topItem.id = "topItem"
            topItem.name = "othername"
            val equippedItem = createItemIn(player)
            equippedItem.id = "equippedItem"
            equippedItem.name = "itemname"
            equippedItem.itemSlot = Some(ItemSlotHead)
            player equip equippedItem

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindInInventory))

            Then("The correct item is returned")
            result shouldBe Some(itemToBeFound)
        }

        "Find an item among the character's equipped items" in {

            Given("A player with 3 equipped items")
            val room = Room()
            val player = createNonPlayerCharacterIn(room)
            val bottomItem = createItemIn(player)
            bottomItem.id = "bottomItem"
            bottomItem.name = "itemname"
            bottomItem.itemSlot = Some(ItemSlotHead)
            player equip bottomItem
            val itemToBeFound = createItemIn(player)
            itemToBeFound.id = "itemToBeFound"
            itemToBeFound.name = "itemname"
            itemToBeFound.itemSlot = Some(ItemSlotFeet)
            player equip itemToBeFound
            val topItem = createItemIn(player)
            topItem.id = "topItem"
            topItem.name = "othername"
            topItem.itemSlot = Some(ItemSlotChest)
            player equip topItem
            val inventoryItem = createItemIn(player)
            inventoryItem.id = "inventoryItem"
            inventoryItem.name = "itemname"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindInEquipped))

            Then("The correct item is returned")
            result shouldBe Some(itemToBeFound)
        }

        "Find an item among all the character's items" in {

            Given("A player with 3 out of 5 items equipped")
            val room = Room()
            val player = createNonPlayerCharacterIn(room)
            val bottomItem = createItemIn(player)
            bottomItem.id = "bottomItem"
            bottomItem.name = "itemname"
            val bottomEquippedItem = createItemIn(player)
            bottomEquippedItem.id = "bottomEquippedItem"
            bottomEquippedItem.name = "itemname"
            bottomEquippedItem.itemSlot = Some(ItemSlotChest)
            player equip bottomEquippedItem
            val itemToBeFound = createItemIn(player)
            itemToBeFound.id = "itemToBeFound"
            itemToBeFound.name = "itemname"
            itemToBeFound.itemSlot = Some(ItemSlotFeet)
            player equip itemToBeFound
            val topEquippedItem = createItemIn(player)
            topEquippedItem.id = "topEquippedItem"
            topEquippedItem.name = "othername"
            topEquippedItem.itemSlot = Some(ItemSlotHands)
            player equip topEquippedItem
            val topItem = createItemIn(player)
            topItem.id = "topItem"
            topItem.name = "itemname"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindInMe))

            Then("The correct item is returned")
            result shouldBe Some(itemToBeFound)
        }

        "Find an item globally" in {

            Given("3 items in another room than the character")
            val playerRoom = Room()
            val player = createNonPlayerCharacterIn(playerRoom)
            val otherRoom = Room()
            val bottomItem = createItemIn(otherRoom)
            bottomItem.id = "bottomItem"
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(otherRoom)
            itemToBeFound.id = "itemToBeFound"
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(otherRoom)
            topItem.id = "topItem"
            topItem.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindGlobally))

            Then("The correct item is returned")
            result shouldBe Some(itemToBeFound)
        }

        "Find an item inside a given container" in {

            Given("A player with 3 items in a container")
            val room = Room()
            val player = createNonPlayerCharacterIn(room)
            val container = createItemIn(player)
            val bottomItem = createItemIn(container)
            bottomItem.id = "bottomItem"
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(container)
            itemToBeFound.id = "itemToBeFound"
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(container)
            topItem.id = "topItem"
            topItem.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Right(container))

            Then("The correct item is returned")
            result shouldBe Some(itemToBeFound)
        }

        "Ignore case" in {

            Given("A player next to 3 items")
            val room = Room()
            val player = createNonPlayerCharacterIn(room)
            val bottomItem = createItemIn(room)
            bottomItem.id = "bottomItem"
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(room)
            itemToBeFound.id = "itemToBeFound"
            itemToBeFound.name = "itemNAME"
            val topItem = createItemIn(room)
            topItem.id = "topItem"
            topItem.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "ITEMname", Left(FindNextToMe))

            Then("The correct item is returned")
            result shouldBe Some(itemToBeFound)
        }

        "Find same name items by a given index" in {

            Given("A player next to 3 items")
            val room = Room()
            val player = createNonPlayerCharacterIn(room)
            val itemToBeFound = createItemIn(room)
            itemToBeFound.id = "itemToBeFound"
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(room)
            topItem.id = "topItem"
            topItem.name = "itemname"

            When("The player searches for the item")
            val result = findUnit(player, "2.itemname", Left(FindNextToMe))

            Then("The correct item is returned")
            result shouldBe Some(itemToBeFound)
        }

        "Not find an item that isn't there" in {

            Given("A player with 3 out of 5 items equipped")
            val room = Room()
            val player = createNonPlayerCharacterIn(room)
            val container = createItemIn(player)
            val itemToNotBeFound = createItemIn(container)
            itemToNotBeFound.id = "itemToNotBeFound"
            itemToNotBeFound.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Right(container))

            Then("The correct item is returned")
            result shouldBe None
        }
    }
}
