package core.gameunit

import core.*
import core.GlobalState.*
import core.gameunit.Position.Standing
import core.commands.*
import core.commands.Commands.{act, executeCommand}
import core.connection.Connection
import core.gameunit.FindContext.*
import core.gameunit.Gender.GenderMale

import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Map as MMap}
import scala.util.{Failure, Success, Try}

sealed trait GameUnit:

    val uuid = UUID.randomUUID()

    var id = ""
    var name = ""
    var title = ""
    var description = ""

    private val _contents = ListBuffer.empty[GameUnit]
    private var _outside: Option[GameUnit] = None

    def contents: List[GameUnit] = _contents.toList

    def outside: Option[GameUnit] = _outside

    def addUnit(unitToAdd: GameUnit): GameUnit =
        unitToAdd.removeUnitFromContainer
        _contents prepend unitToAdd
        unitToAdd._outside = Some(this)
        unitToAdd

    def removeUnit: Unit =
        while contents.nonEmpty do contents.head.removeUnit
        global subtractOne this
        removeUnitFromContainer

    private def removeUnitFromContainer: Unit =
        outside foreach (_._contents subtractOne this)

    def canContain(unit: GameUnit) = true // TODO: check if can contain/carry

    override def equals(other: Any): Boolean = other match {
        case unit: GameUnit => unit.uuid == uuid
        case _ => false
    }

    override def toString: String = this.getClass.getSimpleName + "(" + name + ", " + id + ", " + uuid + ")"

end GameUnit


object GameUnit:

    def createItemIn(container: GameUnit): Item =
        val item = Item()
        container addUnit item
        global prepend item
        item

    def createPlayerCharacterIn(container: GameUnit, connection: Connection): PlayerCharacter =

        val playerCharacter = PlayerCharacter(connection)
        container addUnit playerCharacter
        global prepend playerCharacter
        players prepend playerCharacter

        // TODO: load player data
        playerCharacter.id = "player1"
        playerCharacter.name = "Klaus"
        playerCharacter.title = "the Rude"

        executeCommand(playerCharacter, "look")
        act("$1N has entered the game.", ActVisibility.Always, Some(playerCharacter), None, None, ActRecipient.ToAllExceptActor, None)

        playerCharacter
    end createPlayerCharacterIn

    def createNonPlayerCharacterIn(container: GameUnit): NonPlayerCharacter =
        val nonPlayerCharacter = NonPlayerCharacter()
        container addUnit nonPlayerCharacter
        global prepend nonPlayerCharacter
        nonPlayerCharacter

    def findUnit(character: Character, searchString: String, environment: Either[FindContext, GameUnit]): Option[GameUnit] = {

        val listToSearch = environment match {
            case Left(FindNextToMe) => character.outside.get.contents
            case Left(FindInInventory) => character.inventory
            case Left(FindInEquipped) => character.equippedItems
            case Left(FindInMe) => character.equippedItems concat character.inventory
            case Left(FindInOrNextToMe) => character.equippedItems concat character.inventory concat character.outside.get.contents
            case Left(FindGlobally) => global
            case Right(container) => container.contents
        }

        val (index, name) =
            val terms = searchString.split('.').toList
            Try(terms.head.toInt) match {
                case Success(value) => (value, terms.tail mkString ".")
                case Failure(_) => (1, searchString)
            }

        listToSearch
            .filter(_.name equalsIgnoreCase name)
            .filter(character.canSee)
            .drop(index - 1)
            .headOption
    }

end GameUnit


sealed abstract class Character extends GameUnit :

    private val _equipped = MMap[ItemSlot, Item]()
    private val _equippedReverse = MMap[Item, ItemSlot]()

    var gender: Gender = GenderMale
    var position: Position = Standing
    var doing: Option[(String, TimedCommand)] = None
    var target: Option[Character] = None
    var targetOf: Option[Character] = None

    def equippedItems: List[GameUnit] = contents filter _equipped.values.toList.contains

    def inventory: List[GameUnit] = contents diff _equipped.values.toList

    def equippedAt(itemSlot: ItemSlot): Option[Item] = _equipped get itemSlot

    def equip(item: Item): Option[String] = item.itemSlot match {
        case Some(_) if !(inventory contains item) => Some("You can only equip items from your inventory.")
        case Some(itemSlot) if _equipped contains itemSlot => Some("You already have something equipped there.")
        case Some(itemSlot) => {
            _equipped addOne (itemSlot -> item)
            _equippedReverse addOne (item -> itemSlot)
            None
        }
        case None => Some("This item cannot be equipped.")
    }

    def unequip(item: Item): Option[String] = _equippedReverse remove item match {
        case Some(value) => {
            _equipped remove value
            None
        }
        case None => Some("You don't have that item equipped.")
    }

    def canSee(unit: GameUnit) = true // TODO: implement visibility check

end Character


case class PlayerCharacter private[gameunit](var connection: Connection) extends Character :

    override def removeUnit: Unit =
        super.removeUnit
        players subtractOne this

    def quit(): Unit =
        // TODO: save player data
        this.removeUnit
        connection.close()

end PlayerCharacter


case class NonPlayerCharacter private[gameunit]() extends Character


case class Item private[gameunit]() extends GameUnit :

    var itemSlot: Option[ItemSlot] = None

    def isEquipped: Boolean = (this.itemSlot, this.outside) match {
        case (Some(itemSlot), Some(character: Character)) => character equippedAt itemSlot contains this
        case _ => false
    }

end Item


case class Room() extends GameUnit :

    val exits: MMap[Direction, Exit] = MMap[Direction, Exit]()

    description = "It's a room. There's nothing in it. Not even a door."

    rooms append this

end Room

enum Direction(val display: String):
    case North extends Direction("north")
    case South extends Direction("south")
    case East extends Direction("east")
    case West extends Direction("west")
    case Up extends Direction("up")
    case Down extends Direction("down")


case class Exit(toRoom: Room, distance: Int = 1)


enum ItemSlot(display: String):
    case ItemSlotHead extends ItemSlot("Worn on head")
    case ItemSlotHands extends ItemSlot("Worn on hands")
    case ItemSlotChest extends ItemSlot("Worn on chest")
    case ItemSlotFeet extends ItemSlot("Worn on feet")
    case ItemSlotMainHand extends ItemSlot("Wielded in main-hand")
    case ItemSlotOffHand extends ItemSlot("Wielded in off hand")
    case ItemSlotBothHands extends ItemSlot("Two hand wielded")


enum FindContext:
    case FindNextToMe, FindInEquipped, FindInInventory, FindInMe, FindInOrNextToMe, FindGlobally


enum Gender(e: String, m: String, s: String):
    case GenderMale extends Gender("he", "him", "his")
    case GenderFemale extends Gender("she", "her", "her")
    case GenderNeutral extends Gender("it", "it", "its")


enum Position:
    case Standing, Sitting, Lying
