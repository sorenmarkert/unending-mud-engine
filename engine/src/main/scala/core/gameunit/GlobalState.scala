package core.gameunit

import akka.actor.typed.ActorSystem
import core.*
import core.gameunit.*
import core.gameunit.RunState.Starting

import scala.collection.mutable.{Clearable, LinkedHashMap, ListBuffer}

class GlobalState extends Clearable:

    var runState: RunState = Starting

    val global = ListBuffer[Findable]()
    val rooms = LinkedHashMap[String, Room]()
    val players = LinkedHashMap[String, PlayerCharacter]()

    def clear() = Seq(global, players, rooms) foreach (_.clear)

    val actorSystem = ActorSystem(StateActor(), "unending")


object GlobalState:
    given GlobalState = new GlobalState


enum RunState:
    case Starting, Running, Closing
