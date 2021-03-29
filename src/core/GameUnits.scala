package core

import core.GameState.{global, players}

import java.io.PrintWriter
import java.util.UUID
import scala.collection.mutable.{ListBuffer, Map => MMap}

sealed abstract class GameUnit() {

    val uuid = UUID.randomUUID()

    var id = ""
    var name = ""
    var title = ""
    var description = ""

    private val _contents = ListBuffer[GameUnit]()
    private var _outside: Option[GameUnit] = None

    def contents = _contents

    def outside = _outside

    def addUnit(unitToAdd: GameUnit) = {
        unitToAdd.removeUnitFromContainer
        _contents prepend unitToAdd
        unitToAdd._outside = Some(this)
        unitToAdd
    }

    def removeUnit = {
        global subtractOne this
        removeUnitFromContainer
    }

    private def removeUnitFromContainer = {
        outside foreach (_._contents subtractOne this)
    }
}

object GameUnit {

    def createItemIn(container: GameUnit) = {
        val item = Item()
        container addUnit item
        global prepend item
        item
    }

    def createPlayerCharacterIn(container: GameUnit, connectionState: ConnectionState, writer: PrintWriter) = {
        val playerCharacter = PlayerCharacter(connectionState, writer)
        container addUnit playerCharacter
        global prepend playerCharacter
        players prepend playerCharacter
        playerCharacter
    }

    def createNonPlayerCharacterIn(container: GameUnit) = {
        val nonPlayerCharacter = NonPlayerCharacter()
        container addUnit nonPlayerCharacter
        global prepend nonPlayerCharacter
        nonPlayerCharacter
    }

    def findUnit(character: Character, name: String, environment: Either[FindContext, GameUnit]) = {
        val listToSearch = environment match {
            case Left(FindNextToMe) => character.outside.get.contents
            case Left(FindInInventory) => character.inventory
            case Left(FindInEquipped) => character.equippedItems
            case Left(FindInMe) => character.contents
            case Left(FindInOrNextToMe) => character.contents concat character.outside.get.contents
            case Left(FindGlobally) => global
            case Right(container) => container.contents
        }

        listToSearch find (_.name == name)
    }
}


trait Character extends GameUnit {

    private val _equipped = MMap[ItemSlot, Item]()
    private val _equippedReverse = MMap[Item, ItemSlot]()

    var gender: Gender = GenderMale

    def equippedItems = _equipped.values

    def inventory = contents diff _equipped.values.toList

    def equippedAt(itemSlot: ItemSlot) = _equipped get itemSlot

    def equip(item: Item) = item.itemSlot match {
        case Some(_) if !(inventory contains item) => Some("You can only equip items from your inventory.")
        case Some(itemSlot) if _equipped contains itemSlot => Some("You already have something equipped there.")
        case Some(itemSlot) => {
            _equipped addOne (itemSlot -> item)
            _equippedReverse addOne (item -> itemSlot)
            None
        }
        case None => Some("This item cannot be equipped.")
    }

    def unequip(item: Item) = _equippedReverse remove item match {
        case Some(value) => {
            _equipped remove value
            None
        }
        case None => Some("You don't have that item equipped.")
    }

    def canSee(unit: GameUnit) = true // TODO: implement visibility check
}

case class PlayerCharacter private(var connectionState: ConnectionState, private var writer: PrintWriter) extends Character() {
    override def removeUnit: Unit = {
        super.removeUnit
        players subtractOne this
    }
}

case class NonPlayerCharacter private() extends Character()


case class Item private() extends GameUnit() {

    var itemSlot: Option[ItemSlot] = None

    def isEquipped = (this.itemSlot, this.outside) match {
        case (Some(itemSlot), Some(character: Character)) => character equippedAt itemSlot contains this
        case _ => false
    }
}

case class Room() extends GameUnit {
    val exits = MMap[Direction.Value, Room]()
}


object Direction extends Enumeration {
    val North = Value("north")
    val South = Value("south")
    val East = Value("east")
    val West = Value("west")
    val Up = Value("up")
    val Down = Value("down")
}


sealed trait ItemSlot

case object ItemSlotHead extends ItemSlot

case object ItemSlotHands extends ItemSlot

case object ItemSlotChest extends ItemSlot

case object ItemSlotFeet extends ItemSlot

case object ItemSlotMainHand extends ItemSlot

case object ItemSlotOffHand extends ItemSlot

case object ItemSlotBothHands extends ItemSlot


sealed trait FindContext

case object FindNextToMe extends FindContext

case object FindInEquipped extends FindContext

case object FindInInventory extends FindContext

case object FindInMe extends FindContext

case object FindInOrNextToMe extends FindContext

case object FindGlobally extends FindContext


sealed abstract class Gender

case object GenderMale extends Gender

case object GenderFemale extends Gender

case object GenderNeutral extends Gender
