package core.state

import core.*
import core.gameunit.*
import core.state.RunState.Starting

import scala.collection.mutable.{Clearable, LinkedHashMap, ListBuffer, Map as MMap}

class GlobalState extends Clearable:

    var runState: RunState = Starting

    val items: MMap[String, ListBuffer[Item]] =
        MMap.empty[String, ListBuffer[Item]]
    val nonPlayerCharacters: MMap[String, ListBuffer[NonPlayerCharacter]] =
        MMap.empty[String, ListBuffer[NonPlayerCharacter]]
    val rooms: MMap[String, Room] =
        LinkedHashMap.empty[String, Room]
    val players: MMap[String, PlayerCharacter] =
        LinkedHashMap.empty[String, PlayerCharacter]

    def clear(): Unit = Seq(items, nonPlayerCharacters, players, rooms) foreach (_.clear)


object GlobalState:
    given GlobalState = new GlobalState


enum RunState:
    case Starting, Running, Closing
