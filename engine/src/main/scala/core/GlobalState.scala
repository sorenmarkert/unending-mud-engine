package core

import akka.actor.typed.ActorSystem
import core.RunState.Starting
import core.gameunit.{GameUnit, PlayerCharacter, Room}

import scala.collection.mutable.ListBuffer

object GlobalState:

    var runState: RunState = Starting

    val global  = ListBuffer[GameUnit]()
    val players = ListBuffer[PlayerCharacter]()
    val rooms   = ListBuffer[Room]()

    val actorSystem = ActorSystem(StateActor(), "unending")


enum RunState:
    case Starting, Running, Closing
