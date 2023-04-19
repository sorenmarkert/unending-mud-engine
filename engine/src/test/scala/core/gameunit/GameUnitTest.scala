package core.gameunit

import core.ActRecipient.ToAllExceptActor
import core.ActVisibility.Always
import core.MessageSender
import core.commands.Commands
import core.connection.Connection
import core.gameunit.ItemSlot.*
import core.gameunit.Room
import org.mockito.Mockito.verify
import org.scalatest.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class GameUnitTest extends AnyWordSpec with MockitoSugar with GivenWhenThen with Matchers with BeforeAndAfterEach with OptionValues:

    given globalState: GlobalState = new GlobalState()

    override def beforeEach() =
        globalState.clear()

    "A newly created mobile" should {

        "Be the only mobile in an empty room" in {

            Given("An empty room")
            val room = Room("emptyRoom")

            When("A mobile is created inside it")
            val newMobile = room.createNonPlayerCharacter("newMobile")

            Then("The mobile is the only mobile inside the room")
            room.mobiles should contain only newMobile
            newMobile.outside shouldBe room

            And("On the global list")
            globalState.global should contain only newMobile
        }

        "Be before other mobiles in a non-empty unit" in {

            Given("An room containing an mobile")
            val room      = Room("roomWithMobile")
            val oldMobile = room.createNonPlayerCharacter("oldMobile")

            When("A new mobile is created inside the room")
            val newMobile = room.createNonPlayerCharacter("newMobile")

            Then("The new mobile is the top mobile inside the room")
            room.mobiles should contain theSameElementsInOrderAs Seq(newMobile, oldMobile)
            newMobile.outside shouldBe room

            And("The global list is unchanged")
            globalState.global should contain theSameElementsInOrderAs Seq(newMobile, oldMobile)
        }
    }

    "A destroyed mobile" should {

        "Leave the order of its siblings unchanged" in {

            Given("An room containing three mobiles")
            val room                = Room("roomWith3mobiles")
            val bottomMobile        = room.createNonPlayerCharacter("bottomMobile")
            val mobileToBeDestroyed = room.createNonPlayerCharacter("mobileToBeDestroyed")
            val topMobile           = room.createNonPlayerCharacter("topMobile")

            When("The middle mobile is destroyed")
            mobileToBeDestroyed.destroy

            Then("The two remaining mobiles retain their order in the room")
            room.mobiles should contain theSameElementsInOrderAs Seq(topMobile, bottomMobile)

            And("On the global list")
            globalState.global should contain theSameElementsInOrderAs Seq(topMobile, bottomMobile)
        }

        "Destroy its contents recursively" in {

            Given("An room containing an mobile containing an item")
            val room                = Room("roomWithMobileWithItem")
            val mobileToBeDestroyed = room.createNonPlayerCharacter("mobileToBeDestroyed")
            val containedItem       = mobileToBeDestroyed.createItem("containedItem")

            When("The mobile is destroyed")
            mobileToBeDestroyed.destroy

            Then("The the inner mobile is also destroyed")
            room.mobiles shouldBe empty
            mobileToBeDestroyed.contents shouldBe empty

            And("On the global list")
            globalState.global should contain noElementsOf Set(mobileToBeDestroyed, containedItem)
        }
    }

    "A moved mobile" should {

        "Leave the order of its old siblings unchanged" in {

            Given("An room containing three mobiles")
            val fromRoom        = Room("fromRoom")
            val bottomMobile    = fromRoom.createNonPlayerCharacter("bottomMobile")
            val mobileToBeMoved = fromRoom.createNonPlayerCharacter("mobileToBeMoved")
            val topMobile       = fromRoom.createNonPlayerCharacter("topMobile")
            val toRoom          = Room("toRoom")
            val oldMobile       = toRoom.createNonPlayerCharacter("oldMobile")

            And("The global list contains all the mobiles")
            globalState.global should contain theSameElementsInOrderAs Seq(oldMobile, topMobile, mobileToBeMoved, bottomMobile)

            When("The middle mobile is moved to another room")
            toRoom addMobile mobileToBeMoved

            Then("The two remaining mobiles retain their order in the old room")
            fromRoom.mobiles should contain theSameElementsInOrderAs Seq(topMobile, bottomMobile)

            And("The new mobile is the top mobile inside the new room")
            toRoom.mobiles should contain theSameElementsInOrderAs Seq(mobileToBeMoved, oldMobile)
            mobileToBeMoved.outside shouldBe toRoom

            And("The global list is unchanged")
            globalState.global should contain theSameElementsInOrderAs Seq(oldMobile, topMobile, mobileToBeMoved, bottomMobile)
        }
    }

    "A newly created player character" should {

        "Be the only mobile in an empty room" in {

            given commandsMock: Commands = mock[Commands]

            given messageSenderMock: MessageSender = mock[MessageSender]

            given connectionMock: Connection = mock[Connection]

            Given("An empty room")
            val room = Room("emptyRoom")

            When("A player is created inside it")
            val newPlayer = room.createPlayerCharacter("newPlayer", connectionMock)

            Then("The player is the only mobile inside the room")
            room.mobiles should contain only newPlayer
            newPlayer.outside shouldBe room

            And("On the global players list")
            globalState.players should contain only ("newPlayer" -> newPlayer)

            And("The player looks")
            verify(commandsMock).executeCommand(newPlayer, "look")

            And("")
            verify(messageSenderMock).act("$1N has entered the game.", Always, Some(newPlayer), None, None, ToAllExceptActor, None)
        }
    }

    "A destroyed player character" should {

        "Not be on the global players list" in {

            given commandsMock: Commands = mock[Commands]

            given messageSenderMock: MessageSender = mock[MessageSender]

            given connectionMock: Connection = mock[Connection]

            Given("An room containing a player character")
            val room                = Room("roomWithPlayer")
            val playerToBeDestroyed = room.createPlayerCharacter("playerToBeDestroyed", connectionMock)

            When("The player is destroyed")
            playerToBeDestroyed.destroy

            Then("It's no longer on the global list")
            globalState.players should not contain playerToBeDestroyed
        }
    }

    "A newly created item" should {

        "Be the only item in an empty unit" in {

            Given("An empty room")
            val room = Room("emptyRoom")

            When("An item is created inside it")
            val newItem = room.createItem("newItem")

            Then("The item is the only item inside the room")
            room.contents should contain only newItem
            newItem.outside shouldBe room

            And("On the global list")
            globalState.global should contain only newItem
        }

        "Be before other items in a non-empty unit" in {

            Given("An room containing an item")
            val room    = Room("roomWithItem")
            val oldItem = room.createItem("oldItem")

            When("A new item is created inside the room")
            val newItem = room.createItem("newItem")

            Then("The new item is the top item inside the room")
            room.contents should contain theSameElementsInOrderAs Seq(newItem, oldItem)
            newItem.outside shouldBe room

            And("The global list is unchanged")
            globalState.global should contain theSameElementsInOrderAs Seq(newItem, oldItem)
        }
    }

    "A destroyed item" should {

        "Leave the order of its siblings unchanged" in {

            Given("An room containing three items")
            val room              = Room("roomWith3items")
            val bottomItem        = room.createItem("bottomItem")
            val itemToBeDestroyed = room.createItem("itemToBeDestroyed")
            val topItem           = room.createItem("topItem")

            When("The middle item is destroyed")
            itemToBeDestroyed.destroy

            Then("The two remaining items retain their order in the room")
            room.contents should contain theSameElementsInOrderAs Seq(topItem, bottomItem)

            And("On the global list")
            globalState.global should contain theSameElementsInOrderAs Seq(topItem, bottomItem)
        }

        "Destroy its contents recursively" in {

            Given("An room containing an item containing another item")
            val room              = Room("roomWithItemWithItem")
            val itemToBeDestroyed = room.createItem("itemToBeDestroyed")
            val containedItem     = itemToBeDestroyed.createItem("containedItem")

            When("The outer item is destroyed")
            itemToBeDestroyed.destroy

            Then("The the inner item is also destroyed")
            room.contents shouldBe empty
            itemToBeDestroyed.contents shouldBe empty

            And("On the global list")
            globalState.global should contain noElementsOf Set(itemToBeDestroyed, containedItem)
        }
    }

    "A moved item" should {

        "Leave the order of its old siblings unchanged" in {

            Given("An room containing three items")
            val fromRoom      = Room("fromRoom")
            val bottomItem    = fromRoom.createItem("bottomItem")
            val itemToBeMoved = fromRoom.createItem("itemToBeMoved")
            val topItem       = fromRoom.createItem("topItem")
            val toRoom        = Room("toRoom")
            val oldItem       = toRoom.createItem("oldItem")

            When("The middle item is moved to another room")
            toRoom addItem itemToBeMoved

            Then("The two remaining items retain their order in the old room")
            fromRoom.contents should contain theSameElementsInOrderAs Seq(topItem, bottomItem)

            And("The new item is the top item inside the new room")
            toRoom.contents should contain theSameElementsInOrderAs Seq(itemToBeMoved, oldItem)
            itemToBeMoved.outside shouldBe toRoom

            And("The global list contains all the items")
            globalState.global should contain theSameElementsInOrderAs Seq(oldItem, topItem, itemToBeMoved, bottomItem)
        }
    }

    "Equipping an item" should {

        "Place the item as equipped on the character" in {

            Given("A character with an item")
            val room      = Room("room")
            val character = room.createNonPlayerCharacter("character")
            val item      = character.createItem("item")
            item.itemSlot = Some(ItemSlotHead)

            When("The character equips the item")
            val result = character equip item

            Then("It is placed in the character's equipped items")
            character.equippedItems should contain(item)

            And("The character no longer has the item in the inventory")
            character.inventory should not contain item

            And("The item is equipped")
            item.isEquipped shouldBe true

            And("The item is equipped in its item slot on the character")
            character equippedAt item.itemSlot.get should contain(item)

            And("No error message was returned")
            result shouldBe None
        }

        "Return an error when it's not in the character's inventory" in {

            Given("A character next to an item")
            val room      = Room("room")
            val character = room.createNonPlayerCharacter("character")
            val item      = room.createItem("item")
            item.itemSlot = Some(ItemSlotHead)

            When("The character equips the item")
            val result = character equip item

            Then("An error message is returned")
            result.value shouldBe "You can only equip items from your inventory."
        }

        "Return an error when the character already has an item equipped in that slot" in {

            Given("A character with two items, where one is equipped")
            val room      = Room("room")
            val character = room.createNonPlayerCharacter("character")

            val equippedItem = character.createItem("equippedItem")
            equippedItem.itemSlot = Some(ItemSlotHead)
            character equip equippedItem

            val unequippedItem = character.createItem("unequippedItem")
            unequippedItem.itemSlot = Some(ItemSlotHead)

            When("The character equips the unequipped item")
            val result = character equip unequippedItem

            Then("An error message is returned")
            result.value shouldBe "You already have something equipped there."
        }

        "Return an error when the item is not equippable" in {

            Given("A character an equippable item")
            val room      = Room("room")
            val character = room.createNonPlayerCharacter("character")
            val item      = character.createItem("item")
            item.itemSlot = None

            When("The character equips the item")
            val result = character equip item

            Then("An error message is returned")
            result.value shouldBe "This item cannot be equipped."
        }
    }

    "Removing an item" should {

        "Placed it in the inventory" in {

            Given("A character with an equipped item")
            val room      = Room("room")
            val character = room.createNonPlayerCharacter("character")
            val item      = character.createItem("item")
            item.itemSlot = Some(ItemSlotHead)
            character equip item

            When("The character removes the item")
            val result = character remove item

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
            val character = room.createNonPlayerCharacter("character")
            val item      = room.createItem("item")

            When("The character removes the item")
            val result = character remove item

            Then("An error message is returned")
            result.value shouldBe "You don't have that item equipped."
        }
    }

    "Item.findInside()" should {

        "Find an item inside" in {

            Given("A room with 3 items in a container")
            val room       = Room("room")
            val container  = room.createItem("container")
            val bottomItem = container.createItem("bottomItem")
            bottomItem.name = "itemName"
            val itemToBeFound = container.createItem("itemToBeFound")
            itemToBeFound.name = "itemName"
            val topItem = container.createItem("topItem")
            topItem.name = "otherName"

            When("Searching for the item")
            val result = container.findInside("itemName")

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Not find an item that isn't there" in {

            Given("A player with 3 out of 5 items equipped")
            val room             = Room("room")
            val container        = room.createItem("container")
            val itemNotToBeFound = container.createItem("itemNotToBeFound")
            itemNotToBeFound.name = "otherName"

            When("The player searches for the item")
            val result = container.findInside("itemName")

            Then("No item is returned")
            result shouldBe None
        }

        "Find same name items by a given index" in {

            Given("A room with a container with 3 items")
            val room          = Room("room")
            val container     = room.createItem("container")
            val itemToBeFound = container.createItem("itemToBeFound")
            itemToBeFound.name = "itemName"
            val middleItem = container.createItem("middleItem")
            middleItem.name = "itemName"
            val topItem = container.createItem("topItem")
            topItem.name = "itemName"

            When("The player searches for the third item")
            val result = container.findInside("3.itemname")

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Ignore case and surrounding spaces" in {

            Given("A player next to 3 items")
            val room       = Room("room")
            val container  = room.createItem("container")
            val bottomItem = container.createItem("bottomItem")
            bottomItem.name = "itemName"
            val itemToBeFound = container.createItem("itemToBeFound")
            itemToBeFound.name = "itemNAME"
            val topItem = container.createItem("topItem")
            topItem.name = "otherName"

            When("The player searches for the item")
            val result = container.findInside(" ITEMnaMe ")

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }
    }

    "Room.findItem()" should {

        "Find an item inside" in {

            Given("A room with 3 items")
            val room       = Room("room")
            val bottomItem = room.createItem("bottomItem")
            bottomItem.name = "itemName"
            val itemToBeFound = room.createItem("itemToBeFound")
            itemToBeFound.name = "itemName"
            val topItem = room.createItem("topItem")
            topItem.name = "otherName"

            When("Searching for the item")
            val result = room.findItem("itemName")

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }
    }

    "Room.findMobile()" should {

        "Find a mobile inside" in {

            Given("A room with 3 mobiles")
            val room         = Room("room")
            val bottomMobile = room.createNonPlayerCharacter("bottomMobile")
            bottomMobile.name = "mobileName"
            val mobileToBeFound = room.createNonPlayerCharacter("mobileToBeFound")
            mobileToBeFound.name = "mobileName"
            val topMobile = room.createNonPlayerCharacter("topMobile")
            topMobile.name = "otherName"

            When("Searching for the mobile")
            val result = room.findMobile("mobileName")

            Then("The correct mobile is returned")
            result.value shouldBe mobileToBeFound
        }
    }

    "Mobile.find*()" should {

        "Find an item next to the character" in {

            Given("A mobile next to 3 items")
            val room       = Room("room")
            val mobile     = room.createNonPlayerCharacter("mobile")
            val bottomItem = room.createItem("bottomItem")
            bottomItem.name = "itemName"
            val itemToBeFound = room.createItem("itemToBeFound")
            itemToBeFound.name = "itemName"
            val topItem = room.createItem("topItem")
            topItem.name = "otherName"

            When("The player searches for the item")
            val result = mobile.findItemNextToMe("itemName")

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find an item in the character's inventory" in {

            Given("A mobile with 3 items")
            val room       = Room("room")
            val mobile     = room.createNonPlayerCharacter("mobile")
            val bottomItem = mobile.createItem("bottomItem")
            bottomItem.name = "itemName"
            val itemToBeFound = mobile.createItem("itemToBeFound")
            itemToBeFound.name = "itemName"
            val topItem = mobile.createItem("topItem")
            topItem.name = "otherName"
            val equippedItem = mobile.createItem("equippedItem")
            equippedItem.name = "itemName"
            equippedItem.itemSlot = Some(ItemSlotHead)
            mobile equip equippedItem

            When("The player searches for the item")
            val result = mobile.findInInventory("itemName")

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find an item among the character's equipped items" in {

            Given("A mobile with 3 equipped items")
            val room       = Room("room")
            val mobile     = room.createNonPlayerCharacter("mobile")
            val bottomItem = mobile.createItem("bottomItem")
            bottomItem.name = "itemName"
            bottomItem.itemSlot = Some(ItemSlotHead)
            mobile equip bottomItem
            val itemToBeFound = mobile.createItem("itemToBeFound")
            itemToBeFound.name = "itemName"
            itemToBeFound.itemSlot = Some(ItemSlotFeet)
            mobile equip itemToBeFound
            val topItem = mobile.createItem("topItem")
            topItem.name = "otherName"
            topItem.itemSlot = Some(ItemSlotChest)
            mobile equip topItem
            val inventoryItem = mobile.createItem("inventoryItem")
            inventoryItem.name = "itemName"

            When("The player searches for the item")
            val result = mobile.findInEquipped("itemName")

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find an item among all the character's items" in {

            Given("A mobile with 3 out of 5 items equipped")
            val room       = Room("room")
            val mobile     = room.createNonPlayerCharacter("mobile")
            val bottomItem = mobile.createItem("bottomItem")
            bottomItem.name = "itemName"
            val bottomEquippedItem = mobile.createItem("bottomEquippedItem")
            bottomEquippedItem.name = "itemName"
            bottomEquippedItem.itemSlot = Some(ItemSlotChest)
            mobile equip bottomEquippedItem
            val itemToBeFound = mobile.createItem("itemToBeFound")
            itemToBeFound.name = "itemName"
            itemToBeFound.itemSlot = Some(ItemSlotFeet)
            mobile equip itemToBeFound
            val topEquippedItem = mobile.createItem("topEquippedItem")
            topEquippedItem.name = "otherName"
            topEquippedItem.itemSlot = Some(ItemSlotHands)
            mobile equip topEquippedItem
            val topItem = mobile.createItem("topItem")
            topItem.name = "itemName"

            When("The player searches for the item")
            val result = mobile.findInMe("itemName")

            Then("The correct item is returned")
            result.value shouldBe itemToBeFound
        }

        "Find a sibling mobile" in {

            Given("A room with two mobiles")
            val room            = Room("room")
            val searchingMobile = room.createNonPlayerCharacter("searchingMobile")
            val mobileToBeFound = room.createNonPlayerCharacter("mobileToBeFound")
            mobileToBeFound.name = "mobileName"

            When("One mobile searches for the other mobile")
            val result = searchingMobile.findMobile("mobileName")

            Then("The correct mobile is returned")
            result.value shouldBe mobileToBeFound
        }

        "Find a sibling mobile out of mobiles and all items" in {

            Given("A room with two mobiles")
            val room            = Room("room")
            val searchingMobile = room.createNonPlayerCharacter("searchingMobile")
            val mobileToBeFound = room.createNonPlayerCharacter("mobileToBeFound")
            mobileToBeFound.name = "mobileName"

            When("One mobile searches for the other mobile")
            val result = searchingMobile.findInOrNextToMe("mobileName")

            Then("The correct mobile is returned")
            result.value shouldBe mobileToBeFound
        }
    }
