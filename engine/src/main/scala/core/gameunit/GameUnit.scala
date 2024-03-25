package core.gameunit

import core.*
import core.ActRecipient.ToAllExceptActor
import core.ActVisibility.Always
import core.commands.*
import core.connection.Connection
import core.gameunit.Direction.*
import core.gameunit.Gender.GenderMale
import core.gameunit.Position.Standing

import scala.collection.mutable
import scala.collection.mutable.{LinkedHashMap, ListBuffer, Map as MMap}

type Findable = Mobile | Item


sealed trait GameUnit:

    var title      : String
    var description: String

    private[gameunit] val _contents = ListBuffer.empty[Item]

    def contents = _contents.toSeq

    def addItem(itemToAdd: Item) =
        itemToAdd.removeFromContainer()
        itemToAdd._outside = this
        _contents prepend itemToAdd

    def destroy(using globalState: GlobalState): Unit =
        while _contents.nonEmpty do _contents.head.destroy

    def canContain[T <: GameUnit](unit: Containable[T]) = ??? // TODO: check if can contain/carry

    def createItem(name: String, title: String = "", description: String = "")(using globalState: GlobalState) =
        val item = Item(name, title, description, this)
        _contents prepend item
        globalState.global prepend item
        item

    protected def findUnit[T <: Findable](searchString: String, listToSearch: Seq[T]) =

        lazy val (index, searchStringWithoutIndex) =
            val indexSplit = searchString.split("\\.", 2)
            indexSplit.head.trim.toIntOption match
                case Some(value) => (value, indexSplit.tail.head)
                case None        => (1, searchString)

        lazy val searchTerms = searchStringWithoutIndex.toLowerCase split ' ' filterNot (_.isBlank)

        def matches(names: List[String], terms: List[String]): Boolean =
            (names, terms) match
                case (n :: ns, t :: ts) if n startsWith t => matches(ns, ts)
                case (_ :: ns, t :: ts)                   => matches(ns, t :: ts)
                case (_, Nil)                             => true
                case _                                    => false

        listToSearch
            .filter(u => matches((u.name.toLowerCase split ' ' filterNot (_.isBlank)).toList, searchTerms.toList))
            // TODO: add visibility check: .filter(character.canSee)
            .drop(index - 1)
            .headOption

end GameUnit


sealed trait Containable[In <: GameUnit] extends GameUnit:

    var name: String

    private[gameunit] var _outside: In

    def outside = _outside

    private[gameunit] def removeFromContainer(): Unit


sealed trait Mobile extends Containable[Room]:

    // TODO: bi-map?
    private val _equipped        = LinkedHashMap[ItemSlot, Item]()
    private val _equippedReverse = LinkedHashMap[Item, ItemSlot]()

    override private[gameunit] def removeFromContainer() =
        _outside._mobiles subtractOne this

    var gender  : Gender                         = GenderMale
    var position: Position                       = Standing
    var doing   : Option[(String, TimedCommand)] = None
    var target  : Option[Mobile]                 = None
    var targetOf: Option[Mobile]                 = None

    override def destroy(using globalState: GlobalState) =
        super.destroy
        _equippedReverse.clear()
        while _equipped.nonEmpty do _equipped.head._2.destroy
        removeFromContainer()

    def equippedItems = contents filter _equipped.values.toList.contains

    def inventory = contents diff _equipped.values.toList

    def equippedAt(itemSlot: ItemSlot) = _equipped get itemSlot

    def equip(item: Item): Option[String] =
    // TODO: should equipped items be in contents?
        item.itemSlot match
            case Some(_) if !(inventory contains item)         => Some("You can only equip items from your inventory.")
            case Some(itemSlot) if _equipped contains itemSlot => Some("You already have something equipped there.")
            case Some(itemSlot)                                =>
                _equipped addOne (itemSlot -> item)
                _equippedReverse addOne (item -> itemSlot)
                None
            case None                                          => Some("This item cannot be equipped.")

    def remove(item: Item): Option[String] =
        _equippedReverse remove item match
            case Some(value) =>
                _equipped remove value
                None
            case None        => Some("You don't have that item equipped.")

    def findInInventory(searchString: String) =
        findUnit(searchString, contents)

    def findInEquipped(searchString: String) =
        findUnit(searchString, equippedItems)

    def findInMe(searchString: String) =
        findUnit(searchString, equippedItems concat contents)

    def findItemNextToMe(searchString: String) =
        findUnit(searchString, outside.contents)

    def findItemInOrNextToMe(searchString: String) =
        findUnit(searchString, equippedItems concat contents concat outside.contents)

    def findMobile(searchString: String) =
        findUnit(searchString, outside.mobiles)

    def findInOrNextToMe(searchString: String) =
        findUnit(searchString, outside.mobiles.asInstanceOf[Seq[Findable]]
            concat equippedItems concat contents concat outside.contents)

end Mobile


case class PlayerCharacter private[gameunit](var name: String, var title: String, var description: String, private[gameunit] var _outside: Room, var connection: Connection)
    extends Mobile:

    override def destroy(using globalState: GlobalState) =
        super.destroy
        globalState.players remove name

end PlayerCharacter


case class NonPlayerCharacter private[gameunit](var name: String, var title: String, var description: String, private[gameunit] var _outside: Room)
    extends Mobile:

    override def destroy(using globalState: GlobalState) =
        super.destroy
        globalState.global subtractOne this


// TODO: item templates with item refs
case class Item private[gameunit](var name: String, var title: String, var description: String, private[gameunit] var _outside: GameUnit)
    extends Containable[GameUnit]:

    var itemSlot: Option[ItemSlot] = None

    override private[gameunit] def removeFromContainer() =
        _outside._contents subtractOne this

    override def destroy(using globalState: GlobalState) =
        super.destroy
        removeFromContainer()
        globalState.global subtractOne this

    def isEquipped: Boolean = (itemSlot, outside) match
        case (Some(itemSlot), character: Mobile) => character equippedAt itemSlot contains this
        case _                                   => false

    def findInside(searchString: String) =
        findUnit(searchString, contents)

end Item


case class Room private[gameunit](id: String, var title: String, var description: String) extends GameUnit:

    private val _exits: MMap[Direction, Exit] = MMap[Direction, Exit]()

    def exits = _exits.toMap

    private[gameunit] val _mobiles = ListBuffer.empty[Mobile]

    def mobiles = _mobiles.toSeq

    override def destroy(using globalState: GlobalState) =
        super.destroy
        while _mobiles.nonEmpty do _mobiles.head.destroy
        _exits foreach { case (direction, Exit(toRoom, _)) => toRoom removeLink direction.opposite }
        _exits.clear()

    def addMobile(mobileToAdd: Mobile) =
        mobileToAdd.removeFromContainer()
        mobileToAdd._outside = this
        _mobiles prepend mobileToAdd

    def northTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(North, toRoom, distance, bidirectional)

    def southTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(South, toRoom, distance, bidirectional)

    def eastTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(East, toRoom, distance, bidirectional)

    def westTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(West, toRoom, distance, bidirectional)

    def upTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(Up, toRoom, distance, bidirectional)

    def downTo(toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        addLink(Down, toRoom, distance, bidirectional)

    def addLink(direction: Direction, toRoom: Room, distance: Int = 1, bidirectional: Boolean = true) =
        _exits += direction -> Exit(toRoom, distance)
        if bidirectional then toRoom._exits += direction.opposite -> Exit(this, distance)
        this

    private[gameunit] def removeLink(direction: Direction) =
        _exits remove direction

    def createPlayerCharacter(name: String, connection: Connection)(using globalState: GlobalState, commands: Commands, messageSender: MessageSender): PlayerCharacter =

        // TODO: player to choose name
        // TODO: load player data
        val playerCharacter = PlayerCharacter(name, title, description, this, connection)
        globalState.players += name -> playerCharacter
        _mobiles prepend playerCharacter

        commands.executeCommand(playerCharacter, "look")
        messageSender.act("$1N has entered the game.", Always, Some(playerCharacter), None, None, ToAllExceptActor, None)

        playerCharacter

    def createNonPlayerCharacter(name: String, title: String = "", description: String = "")(using globalState: GlobalState) =
        val nonPlayerCharacter = NonPlayerCharacter(name, title, description, this)
        globalState.global prepend nonPlayerCharacter
        _mobiles prepend nonPlayerCharacter
        nonPlayerCharacter

    def findItem(searchString: String) =
        findUnit(searchString, contents)

    def findMobile(searchString: String) =
        findUnit(searchString, mobiles)

end Room


object Room:

    def apply(id: String, title: String = "")(using globalState: GlobalState): Room =
        import globalState.rooms
        rooms.get(id)
            .map(_ => throw IllegalStateException(s"Room '$id' is already defined."))
            .getOrElse
        val newRoom = Room(id, title, "It's a room. There's nothing in it. Not even a door.")
        rooms += id -> newRoom
        newRoom

end Room
