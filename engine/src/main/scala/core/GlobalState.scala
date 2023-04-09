package core

import akka.actor.typed.ActorSystem
import core.RunState.Starting
import core.gameunit.*

import scala.collection.mutable
import scala.collection.mutable.{Clearable, ListBuffer, Map as MMap}

class GlobalState:

    var runState: RunState = Starting

    val global  = ListBuffer[GameUnit]()
    val players = ListBuffer[PlayerCharacter]() // TODO: LinkedHashMap?
    val rooms   = MMap[String, Room]()

    def clear() = Seq(global, players, rooms) map (_.clear)

    val actorSystem = ActorSystem(StateActor(), "unending")


object GlobalState:
    given GlobalState = new GlobalState


enum RunState:
    case Starting, Running, Closing
