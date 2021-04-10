package core

import scala.collection.mutable.ListBuffer

object GameState {

    sealed trait RunState

    case object Starting extends RunState

    case object Running extends RunState

    case object Closing extends RunState

    var runState: RunState = Starting

    val global = ListBuffer[GameUnit]()
    val players = ListBuffer[PlayerCharacter]()
    val rooms = ListBuffer[Room]()
}
