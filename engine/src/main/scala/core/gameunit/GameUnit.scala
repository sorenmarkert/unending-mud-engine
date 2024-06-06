package core.gameunit

import akka.actor.typed.ActorSystem
import core.*
import core.commands.*
import core.commands.Commands.TimedCommand
import core.connection.Connection
import core.gameunit.Direction.*
import core.gameunit.Gender.GenderMale
import core.gameunit.Position.Standing
import core.state.{GlobalState, StateActor}

import java.util.UUID
import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.{LinkedHashMap, ListBuffer, Map as MMap}

type Findable = Mobile | Item


sealed trait GameUnit:

    val uuid: UUID = UUID.randomUUID()

    var title: String
    var description: String

    private[gameunit] val _contents = ListBuffer.empty[Item]

    def contents: Seq[Item] = _contents.toSeq

    def addItem(itemToAdd: Item): _contents.type =
        itemToAdd.removeFromContainer()
        itemToAdd._outside = this
        _contents prepend itemToAdd

    def destroy(using globalState: GlobalState, actorSystem: ActorSystem[StateActor.Message]): Unit =
        while _contents.nonEmpty do _contents.head.destroy

    def canContain[T <: GameUnit](unit: Containable[T]): Boolean = ???

    def createItem(name: String, title: String = "", description: String = "")(using globalState: GlobalState): Item =
        val item = Item(name, title, description, this)
        _contents prepend item
        globalState.items.getOrElseUpdate(name, ListBuffer()) prepend item
        item

    protected def findUnit[T <: Findable](searchString: String, listToSearch: Seq[T]): Option[T] =

        lazy val (index, searchStringWithoutIndex) =
            val indexSplit = searchString.split("\\.", 2)
            indexSplit.head.trim.toIntOption match
                case Some(value) => (value, indexSplit.tail.head)
                case None => (1, searchString)

        lazy val searchTerms = searchStringWithoutIndex.toLowerCase.split(' ').filterNot(_.isBlank)

        @tailrec
        def matches(names: List[String], terms: List[String]): Boolean =
            (names, terms) match
                case (name :: ns, term :: ts) if name.startsWith(term) => matches(ns, ts)
                case (_ :: ns, t :: ts) => matches(ns, t :: ts)
                case (_, Nil) => true
                case _ => false

        listToSearch
            .filter(u => matches(u.name.toLowerCase.split(' ').filterNot(_.isBlank).toList, searchTerms.toList))
            .drop(index - 1)
            .headOption

end GameUnit


sealed trait Containable[In <: GameUnit] extends GameUnit:

    var name: String

    private[gameunit] var _outside: In

    def outside: In = _outside

    private[gameunit] def removeFromContainer(): Unit


sealed trait Mobile extends Containable[Room]:

    private val _equipped = LinkedHashMap[ItemSlot, Item]()

    override private[gameunit] def removeFromContainer(): Unit =
        _outside._mobiles subtractOne this

    var gender: Gender = GenderMale
    var position: Position = Standing
    var doing: Option[(String, TimedCommand)] = None
    var target: Option[Mobile] = None
    var targetOf: Option[Mobile] = None

    override def destroy(using globalState: GlobalState, actorSystem: ActorSystem[StateActor.Message]): Unit =
        super.destroy
        _equipped.iterator.foreach { case (_, item) => item.destroy}
        removeFromContainer()
        actorSystem tell StateActor.Destroy(this)

    def equippedItems: Seq[Item] = contents filter _equipped.values.toList.contains

    def inventory: Seq[Item] = contents diff _equipped.values.toList

    def equippedAt(itemSlot: ItemSlot): Option[Item] = _equipped get itemSlot

    def equip(item: Item): Option[String] =
        item.itemSlot match
            case Some(_) if !(inventory contains item) => Some("You can only equip items from your inventory.")
            case Some(itemSlot) if _equipped contains itemSlot => Some("You already have something equipped there.")
            case Some(itemSlot) =>
                _equipped addOne (itemSlot -> item)
                None
            case None => Some("This item cannot be equipped.")

    def remove(item: Item): Option[String] =
        item.itemSlot
            .map(_equipped.remove) match
                case Some(_) => None
                case None => Some("You don't have that item equipped.")

    def findInInventory(searchString: String): Option[Item] =
        findUnit(searchString, contents)

    def findInEquipped(searchString: String): Option[Item] =
        findUnit(searchString, equippedItems)

    def findInMe(searchString: String): Option[Item] =
        findUnit(searchString, equippedItems concat contents)

    def findItemNextToMe(searchString: String): Option[Item] =
        findUnit(searchString, outside.contents)

    def findItemInOrNextToMe(searchString: String): Option[Item] =
        findUnit(searchString, equippedItems concat contents concat outside.contents)

    def findMobile(searchString: String): Option[Mobile] =
        findUnit(searchString, outside.mobiles)

    def findInOrNextToMe(searchString: String): Option[Findable] =
        findUnit(searchString, outside.mobiles.asInstanceOf[Seq[Findable]]
            concat equippedItems concat contents concat outside.contents)

end Mobile


case class PlayerCharacter private[gameunit](var name: String, var title: String, var description: String, private[gameunit] var _outside: Room, var connection: Connection)
    extends Mobile:

    override def destroy(using globalState: GlobalState, actorSystem: ActorSystem[StateActor.Message]): Unit =
        super.destroy
        globalState.players remove name

end PlayerCharacter


case class NonPlayerCharacter private[gameunit](var name: String, var title: String, var description: String, private[gameunit] var _outside: Room)
    extends Mobile:

    override def destroy(using globalState: GlobalState, actorSystem: ActorSystem[StateActor.Message]): Unit =
        super.destroy
        val charactersWithName = globalState.nonPlayerCharacters(name) subtractOne this
        if charactersWithName.isEmpty then
            globalState.nonPlayerCharacters.remove(name)


case class Item private[gameunit](var name: String, var title: String, var description: String, private[gameunit] var _outside: GameUnit)
    extends Containable[GameUnit]:

    var itemSlot: Option[ItemSlot] = None

    override private[gameunit] def removeFromContainer(): Unit =
        _outside._contents subtractOne this

    override def destroy(using globalState: GlobalState, actorSystem: ActorSystem[StateActor.Message]): Unit =
        super.destroy
        removeFromContainer()
        val itemsWithName = globalState.items(name) subtractOne this
        if itemsWithName.isEmpty then
            globalState.items.remove(name)

    def isEquipped: Boolean = (itemSlot, outside) match
        case (Some(itemSlot), character: Mobile) => character.equippedAt(itemSlot) contains this
        case _ => false

    def findInside(searchString: String): Option[Item] =
        findUnit(searchString, contents)

end Item


case class Room private[gameunit](id: String, var title: String, var description: String) extends GameUnit:

    private val _exits: MMap[Direction, Exit] = MMap[Direction, Exit]()

    def exits: Map[Direction, Exit] = _exits.toMap

    private[gameunit] val _mobiles = ListBuffer.empty[Mobile]

    def mobiles: Seq[Mobile] = _mobiles.toSeq

    override def destroy(using globalState: GlobalState, actorSystem: ActorSystem[StateActor.Message]): Unit =
        super.destroy
        while _mobiles.nonEmpty do _mobiles.head.destroy
        _exits foreach { case (direction, Exit(toRoom, _)) => toRoom.removeLink(direction.opposite) }
        _exits.clear()

    def addMobile(mobileToAdd: Mobile): _mobiles.type =
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

    def addLink(direction: Direction, toRoom: Room, distance: Int = 1, bidirectional: Boolean = true): Room =
        _exits += direction -> Exit(toRoom, distance)
        if bidirectional then toRoom._exits += direction.opposite -> Exit(this, distance)
        this

    private[gameunit] def removeLink(direction: Direction) =
        _exits remove direction

    def createPlayerCharacter(name: String, connection: Connection)(using globalState: GlobalState, commands: Commands, messageSender: MessageSender): PlayerCharacter =

        val capitalizedName = name.toLowerCase.capitalize
        val playerCharacter = PlayerCharacter(capitalizedName, title, description, this, connection)
        globalState.players(capitalizedName) = playerCharacter
        _mobiles prepend playerCharacter

        commands.executeCommandAtNextTick(playerCharacter, "look")
        messageSender.sendMessageToBystandersOf(playerCharacter, s"${playerCharacter.name} has entered the game.")

        playerCharacter

    def createNonPlayerCharacter(name: String, title: String = "", description: String = "")(using globalState: GlobalState): NonPlayerCharacter =
        val nonPlayerCharacter = NonPlayerCharacter(name, title, description, this)
        globalState.nonPlayerCharacters.getOrElseUpdate(name, ListBuffer())
            .prepend(nonPlayerCharacter)
        _mobiles prepend nonPlayerCharacter
        nonPlayerCharacter

    def findItem(searchString: String): Option[Item] =
        findUnit(searchString, contents)

    def findMobile(searchString: String): Option[Mobile] =
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
