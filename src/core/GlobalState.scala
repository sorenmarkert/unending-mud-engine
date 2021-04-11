package core

import akka.actor.typed.ActorSystem

import scala.collection.mutable.ListBuffer

object GlobalState {

    sealed trait RunState

    case object Starting extends RunState

    case object Running extends RunState

    case object Closing extends RunState

    var runState: RunState = Starting


    val global = ListBuffer[GameUnit]()

    val players = ListBuffer[PlayerCharacter]()

    val rooms = ListBuffer[Room]()

    val actorSystem = ActorSystem(StateActor(), "unending")
}
