package core

import core.GameState.global

import java.io.PrintWriter
import java.util.UUID
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.ListBuffer

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

    def createPlayerCharacter(container: GameUnit, connectionState: ConnectionState, writer: PrintWriter) = {
        val playerCharacter = PlayerCharacter(connectionState, writer)
        container addUnit playerCharacter
        global prepend playerCharacter
        playerCharacter
    }

    def createNonPlayerCharacter(container: GameUnit) = {
        val nonPlayerCharacter = NonPlayerCharacter()
        container addUnit nonPlayerCharacter
        global prepend nonPlayerCharacter
        nonPlayerCharacter
    }

    def findUnit(character: Character, name: String, environment: Either[FindContext, GameUnit]) = ???
}

trait Character extends GameUnit

case class PlayerCharacter private(var connectionState: ConnectionState, private var writer: PrintWriter) extends Character()

case class NonPlayerCharacter private() extends Character()

case class Item private() extends GameUnit() {
    var itemType = ""
}

case class Room() extends GameUnit {
    val exits = MMap[Direction, Room]()
}

sealed abstract class Direction(val name: String)

case object North extends Direction("north")

case object South extends Direction("south")

case object East extends Direction("east")

case object West extends Direction("west")

case object Up extends Direction("up")

case object Down extends Direction("down")

sealed trait FindContext

case object FindNextToMe extends FindContext

case object FindInEquipped extends FindContext

case object FindInInventory extends FindContext

case object FindInMe extends FindContext

case object FindGlobally extends FindContext
