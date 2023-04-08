package core.gameunit

import core.GlobalState
import core.GlobalState.global
import core.gameunit.FindContext.*
import core.gameunit.GameUnit.*
import core.gameunit.ItemSlot.*
import core.gameunit.Room
import org.scalatest.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GameUnitTest extends AnyWordSpec with GivenWhenThen with Matchers with BeforeAndAfterEach with OptionValues:

    override def beforeEach() =
        GlobalState.clear()

    "A newly created item" should {

        "be the only unit in an empty unit" in {

            Given("An empty room")
            val room = Room("emptyRoom")

            When("An item is created inside it")
            val newItem = createItemIn(room, "newItem")

            Then("The item is the only thing inside the room")
            room.contents should contain only newItem
            newItem.outside.value shouldBe room

            And("On the global list")
            global should contain only newItem
        }

        "be before other units in a non-empty unit" in {

            Given("An room containing an item")
            val room    = Room("roomWithItem")
            val oldItem = createItemIn(room, "oldItem")

            When("A new item is created inside the room")
            val newItem = createItemIn(room, "newItem")

            Then("The new item is the top thing inside the room")
            room.contents should contain theSameElementsInOrderAs Seq(newItem, oldItem)
            newItem.outside.value shouldBe room

            And("On the global list")
            global should contain theSameElementsInOrderAs Seq(newItem, oldItem)
        }
    }

    "A deleted item" should {

        "Leave the order of its siblings unchanged" in {

            Given("An room containing three items")
            val room            = Room("roomWith3items")
            val bottomItem      = createItemIn(room, "bottomItem")
            val itemToBeDeleted = createItemIn(room, "itemToBeDeleted")
            val topItem         = createItemIn(room, "topItem")

            When("The middle item is removed")
            itemToBeDeleted.removeUnit

            Then("The two remaining items retain their order in the room")
            room.contents should contain theSameElementsInOrderAs Seq(topItem, bottomItem)

            And("On the global list")
            global should contain theSameElementsInOrderAs Seq(topItem, bottomItem)
        }

        "Delete its contents recursively" in {

            Given("An room containing an item containing another item")
            val room            = Room("roomWithItemWithItem")
            val itemToBeDeleted = createItemIn(room, "itemToBeDeleted")
            val containedItem   = createItemIn(itemToBeDeleted, "containedItem")

            When("The outer item is removed")
            itemToBeDeleted.removeUnit

            Then("The the inner item is also removed")
            room.contents shouldBe empty
            itemToBeDeleted.contents shouldBe empty

            And("On the global list")
            global should contain noElementsOf Set(itemToBeDeleted, containedItem)
        }
    }

    "A moved item" should {

        "Leave the order of its old siblings unchanged" in {

            Given("An room containing three items")
            val fromRoom      = Room("fromRoom")
            val bottomItem    = createItemIn(fromRoom, "bottomItem")
            val itemToBeMoved = createItemIn(fromRoom, "itemToBeMoved")
            val topItem       = createItemIn(fromRoom, "topItem")
            val toRoom        = Room("toRoom")
            val oldItem       = createItemIn(toRoom, "oldItem")

            When("The middle item is moved to another room")
            toRoom addUnit itemToBeMoved

            Then("The two remaining items retain their order in the old room")
            fromRoom.contents should contain theSameElementsInOrderAs Seq(topItem, bottomItem)

            And("The new item is the top thing inside the new room")
            toRoom.contents should contain theSameElementsInOrderAs Seq(itemToBeMoved, oldItem)
            itemToBeMoved.outside.value shouldBe toRoom

            And("The global list contains all the items")
            global should contain theSameElementsInOrderAs Seq(oldItem, topItem, itemToBeMoved, bottomItem)
        }
    }

    "Equipping an item" should {

        "Place the item as equipped on the character" in {

            Given("A character with an item")
            val room      = Room("room")
            val character = createNonPlayerCharacterIn(room, "character")
            val item      = createItemIn(character, "item")
            item.itemSlot = Some(ItemSlotHead)

            When("The character equips the item")
            val result = character equip item

            Then("It is placed in the character's equipped items")
            character.equippedItems should contain(item)

            And("The character no longer has the item in the inventory")
            character.inventory should not contain item

            And("The item is equipped")
            item.isEquipped shouldBe true

            And("No error message was returned")
            result shouldBe None
        }

        "Return an error when it's not in the character's inventory" in {

            Given("A character next to an item")
            val room      = Room("room")
            val character = createNonPlayerCharacterIn(room, "character")
            val item      = createItemIn(room, "val item = createItemIn(room)")
            item.itemSlot = Some(ItemSlotHead)

            When("The character equips the item")
            val result = character equip item

            Then("An error message is returned")
            result.value shouldBe "You can only equip items from your inventory."
        }

        "Return an error when the character already has an item equipped in that slot" in {

            Given("A character with two item, where one is equipped")
            val room      = Room("room")
            val character = createNonPlayerCharacterIn(room, "character")

            val equippedItem = createItemIn(character, "equippedItem")
            equippedItem.itemSlot = Some(ItemSlotHead)
            character equip equippedItem

            val unequippedItem = createItemIn(character, "unequippedItem")
            unequippedItem.itemSlot = Some(ItemSlotHead)

            When("The character equips the unequipped item")
            val result = character equip unequippedItem

            Then("An error message is returned")
            result.value shouldBe "You already have something equipped there."
        }

        "Return an error when the item is unequippable" in {

            Given("A character with two item, where one is equipped")
            val room      = Room("room")
            val character = createNonPlayerCharacterIn(room, "character")
            val item      = createItemIn(character, "item")
            item.itemSlot = None

            When("The character equips the unequipped item")
            val result = character equip item

            Then("An error message is returned")
            result.value shouldBe "This item cannot be equipped."
        }
    }

    "Unequipping an item" should {

        "Placed it in the inventory" in {

            Given("A character with an equipped item")
            val room      = Room("room")
            val character = createNonPlayerCharacterIn(room, "character")
            val item      = createItemIn(character, "item")
            item.itemSlot = Some(ItemSlotHead)
            character equip item

            When("The character unequips the item")
            val result = character unequip item

            Then("It is placed in the character's inventory")
            character.inventory should contain(item)

            And("The character no longer has the item equipped")
            character.equippedItems should not contain item

            And("The item is not equipped")
            item.isEquipped shouldBe false

            And("No error message was returned")
            result shouldBe None
        }

        "Return an error when it's not equipped" in {

            Given("A character next to an item")
            val room      = Room("room")
            val character = createNonPlayerCharacterIn(room, "character")
            val item      = createItemIn(room, "val item = createItemIn(room)")

            When("The character unequips the item")
            val result = character unequip item

            Then("An error message is returned")
            result.value shouldBe "You don't have that item equipped."
        }
    }

    "findUnit" should {

        "Find an item next to the character" in {

            Given("A player next to 3 items")
            val room       = Room("room")
            val player     = createNonPlayerCharacterIn(room, "player")
            val bottomItem = createItemIn(room, "bottomItem")
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(room, "itemToBeFound")
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(room, "topItem")
            topItem.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindNextToMe))

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find an item in the character's inventory" in {

            Given("A player with 3 items")
            val room       = Room("room")
            val player     = createNonPlayerCharacterIn(room, "player")
            val bottomItem = createItemIn(player, "bottomItem")
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(player, "itemToBeFound")
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(player, "topItem")
            topItem.name = "othername"
            val equippedItem = createItemIn(player, "equippedItem")
            equippedItem.name = "itemname"
            equippedItem.itemSlot = Some(ItemSlotHead)
            player equip equippedItem

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindInInventory))

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find an item among the character's equipped items" in {

            Given("A player with 3 equipped items")
            val room       = Room("room")
            val player     = createNonPlayerCharacterIn(room, "player")
            val bottomItem = createItemIn(player, "bottomItem")
            bottomItem.name = "itemname"
            bottomItem.itemSlot = Some(ItemSlotHead)
            player equip bottomItem
            val itemToBeFound = createItemIn(player, "itemToBeFound")
            itemToBeFound.name = "itemname"
            itemToBeFound.itemSlot = Some(ItemSlotFeet)
            player equip itemToBeFound
            val topItem = createItemIn(player, "topItem")
            topItem.name = "othername"
            topItem.itemSlot = Some(ItemSlotChest)
            player equip topItem
            val inventoryItem = createItemIn(player, "inventoryItem")
            inventoryItem.name = "itemname"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindInEquipped))

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find an item among all the character's items" in {

            Given("A player with 3 out of 5 items equipped")
            val room       = Room("room")
            val player     = createNonPlayerCharacterIn(room, "player")
            val bottomItem = createItemIn(player, "bottomItem")
            bottomItem.name = "itemname"
            val bottomEquippedItem = createItemIn(player, "bottomEquippedItem")
            bottomEquippedItem.name = "itemname"
            bottomEquippedItem.itemSlot = Some(ItemSlotChest)
            player equip bottomEquippedItem
            val itemToBeFound = createItemIn(player, "itemToBeFound")
            itemToBeFound.name = "itemname"
            itemToBeFound.itemSlot = Some(ItemSlotFeet)
            player equip itemToBeFound
            val topEquippedItem = createItemIn(player, "topEquippedItem")
            topEquippedItem.name = "othername"
            topEquippedItem.itemSlot = Some(ItemSlotHands)
            player equip topEquippedItem
            val topItem = createItemIn(player, "topItem")
            topItem.name = "itemname"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindInMe))

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find an item globally" in {

            Given("3 items in another room than the character")
            val playerRoom = Room("room")
            val player     = createNonPlayerCharacterIn(playerRoom, "player")
            val otherRoom  = Room("Some Title")
            val bottomItem = createItemIn(otherRoom, "bottomItem")
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(otherRoom, "itemToBeFound")
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(otherRoom, "topItem")
            topItem.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Left(FindGlobally))

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find an item inside a given container" in {

            Given("A player with 3 items in a container")
            val room       = Room("room")
            val player     = createNonPlayerCharacterIn(room, "player")
            val container  = createItemIn(player, "val container = createItemIn(player)")
            val bottomItem = createItemIn(container, "bottomItem")
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(container, "itemToBeFound")
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(container, "topItem")
            topItem.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Right(container))

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Ignore case" in {

            Given("A player next to 3 items")
            val room       = Room("room")
            val player     = createNonPlayerCharacterIn(room, "player")
            val bottomItem = createItemIn(room, "bottomItem")
            bottomItem.name = "itemname"
            val itemToBeFound = createItemIn(room, "itemToBeFound")
            itemToBeFound.name = "itemNAME"
            val topItem = createItemIn(room, "topItem")
            topItem.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "ITEMname", Left(FindNextToMe))

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find same name items by a given index" in {

            Given("A player next to 3 items")
            val room          = Room("room")
            val player        = createNonPlayerCharacterIn(room, "player")
            val itemToBeFound = createItemIn(room, "itemToBeFound")
            itemToBeFound.name = "itemname"
            val topItem = createItemIn(room, "topItem")
            topItem.name = "itemname"

            When("The player searches for the item")
            val result = findUnit(player, "2.itemname", Left(FindNextToMe))

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Not find an item that isn't there" in {

            Given("A player with 3 out of 5 items equipped")
            val room             = Room("room")
            val player           = createNonPlayerCharacterIn(room, "player")
            val container        = createItemIn(player, "val container = createItemIn(player)")
            val itemToNotBeFound = createItemIn(container, "itemToNotBeFound")
            itemToNotBeFound.name = "othername"

            When("The player searches for the item")
            val result = findUnit(player, "itemname", Right(container))

            Then("The correct item is returned")
            result shouldBe None
        }
    }
