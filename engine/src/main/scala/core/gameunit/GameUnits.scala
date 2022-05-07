package core.gameunit

import core.*
import core.GlobalState.*
import core.commands.*
import core.commands.Commands.{act, executeCommand}
import core.connection.Connection
import core.gameunit.Direction.*
import core.gameunit.FindContext.*
import core.gameunit.Gender.GenderMale
import core.gameunit.Position.Standing

import java.util.UUID
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Map as MMap}
import scala.util.{Failure, Success, Try}

sealed trait GameUnit(val id: String):

    val uuid = UUID.randomUUID()

    var name        = ""
    var title       = ""
    var description = ""

    private val _contents                  = ListBuffer.empty[GameUnit]
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
        case _              => false
    }

    override def toString: String = this.getClass.getSimpleName + "(" + name + ", " + id + ", " + uuid + ")"

end GameUnit


object GameUnit:

    def createItemIn(container: GameUnit, id: String): Item =
        val item = Item(id)
        container addUnit item
        global prepend item
        item

    def createPlayerCharacterIn(container: GameUnit, connection: Connection): PlayerCharacter =

        // TODO: load player data
        val playerCharacter = PlayerCharacter("player1", connection)
        container addUnit playerCharacter
        global prepend playerCharacter
        players prepend playerCharacter

        playerCharacter.name = "Klaus"
        playerCharacter.title = "the Rude"

        executeCommand(playerCharacter, "look")
        act("$1N has entered the game.", ActVisibility.Always, Some(playerCharacter), None, None, ActRecipient.ToAllExceptActor, None)

        playerCharacter
    end createPlayerCharacterIn

    def createNonPlayerCharacterIn(container: GameUnit, id: String): NonPlayerCharacter =
        val nonPlayerCharacter = NonPlayerCharacter(id)
        container addUnit nonPlayerCharacter
        global prepend nonPlayerCharacter
        nonPlayerCharacter

    def findUnit(character: Character, searchString: String, environment: Either[FindContext, GameUnit]): Option[GameUnit] = {

        val listToSearch = environment match {
            case Left(FindNextToMe)     => character.outside.get.contents
            case Left(FindInInventory)  => character.inventory
            case Left(FindInEquipped)   => character.equippedItems
            case Left(FindInMe)         => character.equippedItems concat character.inventory
            case Left(FindInOrNextToMe) => character.equippedItems concat character.inventory concat character.outside.get.contents
            case Left(FindGlobally)     => global
            case Right(container)       => container.contents
        }

        val (index, name) =
            val terms = searchString.split('.').toList
            Try(terms.head.toInt) match {
                case Success(value) => (value, terms.tail mkString ".")
                case Failure(_)     => (1, searchString)
            }

        listToSearch
            .filter(_.name equalsIgnoreCase name)
            .filter(character.canSee)
            .drop(index - 1)
            .headOption
    }

end GameUnit


sealed abstract class Character(val _id: String) extends GameUnit(_id) :

    private val _equipped        = MMap[ItemSlot, Item]()
    private val _equippedReverse = MMap[Item, ItemSlot]()

    var gender  : Gender                         = GenderMale
    var position: Position                       = Standing
    var doing   : Option[(String, TimedCommand)] = None
    var target  : Option[Character]              = None
    var targetOf: Option[Character]              = None

    def equippedItems: List[GameUnit] = contents filter _equipped.values.toList.contains

    def inventory: List[GameUnit] = contents diff _equipped.values.toList

    def equippedAt(itemSlot: ItemSlot): Option[Item] = _equipped get itemSlot

    def equip(item: Item): Option[String] =
        item.itemSlot match {
            case Some(_) if !(inventory contains item)         => Some("You can only equip items from your inventory.")
            case Some(itemSlot) if _equipped contains itemSlot => Some("You already have something equipped there.")
            case Some(itemSlot)                                =>
                _equipped addOne (itemSlot -> item)
                _equippedReverse addOne (item -> itemSlot)
                None
            case None                                          => Some("This item cannot be equipped.")
        }

    def unequip(item: Item): Option[String] =
        _equippedReverse remove item match {
            case Some(value) => {
                _equipped remove value
                None
            }
            case None        => Some("You don't have that item equipped.")
        }

    def canSee(unit: GameUnit) = true // TODO: implement visibility check

end Character


case class PlayerCharacter private[gameunit](__id: String, var connection: Connection) extends Character(__id) :

    override def removeUnit: Unit =
        super.removeUnit
        players subtractOne this

    def quit(): Unit =
        // TODO: save player data
        this.removeUnit
        connection.close()

end PlayerCharacter


case class NonPlayerCharacter private[gameunit](__id: String) extends Character(__id)


case class Item private[gameunit](_id: String) extends GameUnit(_id) :

    var itemSlot: Option[ItemSlot] = None

    def isEquipped: Boolean = (this.itemSlot, this.outside) match {
        case (Some(itemSlot), Some(character: Character)) => character equippedAt itemSlot contains this
        case _                                            => false
    }

end Item


case class Room private(_id: String) extends GameUnit(_id) :

    // TODO: builder pattern
    private val _exits: MMap[Direction, Exit] = MMap[Direction, Exit]()

    def exits: Map[Direction, Exit] = _exits.toMap

    def withTitle(title: String): Room =
        this.title = title
        this

    def withDescription(description: String): Room =
        this.description = description
        this

    def northTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(toRoom, distance, bidirectional, North)

    def southTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(toRoom, distance, bidirectional, South)

    def eastTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(toRoom, distance, bidirectional, East)

    def westTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(toRoom, distance, bidirectional, West)

    def upTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(toRoom, distance, bidirectional, Up)

    def downTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(toRoom, distance, bidirectional, Down)

    private def addLink(toRoom: Room, distance: Int, bidirectional: Boolean, direction: Direction) =
        _exits += (direction -> Exit(toRoom, distance))
        if bidirectional then toRoom._exits += direction.opposite -> Exit(this, distance)
        this

end Room


object Room:

    def apply(id: String): Room =
        val newRoom = new Room(id)
        rooms append newRoom
        newRoom.description = "It's a room. There's nothing in it. Not even a door."
        newRoom

end Room