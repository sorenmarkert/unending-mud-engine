package core.gameunit

import akka.actor.typed.ActorSystem
import core.*
import core.gameunit.*
import core.gameunit.RunState.Starting

import scala.collection.mutable
import scala.collection.mutable.{Clearable, LinkedHashMap, ListBuffer}

class GlobalState extends Clearable:

    var runState: RunState = Starting

    val global: ListBuffer[Findable] = ListBuffer[Findable]()
    val rooms: mutable.Map[String, Room] = mutable.LinkedHashMap[String, Room]()
    val players: mutable.Map[String, PlayerCharacter] = mutable.LinkedHashMap[String, PlayerCharacter]()

    def clear(): Unit = Seq(global, players, rooms) foreach (_.clear)

    val actorSystem: ActorSystem[StateActorMessage] = ActorSystem(StateActor(), "unending")


object GlobalState:
    given GlobalState = new GlobalState


enum RunState:
    case Starting, Running, Closing
