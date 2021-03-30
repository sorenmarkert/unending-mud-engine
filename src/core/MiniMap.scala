package core

import core.Direction.{East, North, South, West}

import scala.collection.mutable.{Set => MSet}

object MiniMap {

    def miniMap(room: Room, range: Int): List[String] = {

        val theMap = Array.tabulate(4 * range + 1, 6 * range + 1)((_, _) => ' ')
        val traversedRooms = MSet[Room]()

        def mapRoom(currentRoom: Room, y: Int, x: Int, distance: Int): Unit = {

            if (distance < range && !(traversedRooms contains currentRoom)) {
                traversedRooms += currentRoom

                val directionsToMap = Set(North, South, East, West) intersect currentRoom.exits.keySet
                directionsToMap.foreach {
                    case North =>
                        theMap(y - 1)(x) = '|'
                        theMap(y - 2)(x) = '#'
                        mapRoom(currentRoom.exits(North), y - 2, x, distance + 1)
                    case South =>
                        theMap(y + 1)(x) = '|'
                        theMap(y + 2)(x) = '#'
                        mapRoom(currentRoom.exits(South), y + 2, x, distance + 1)
                    case East =>
                        theMap(y)(x + 1) = '-'
                        theMap(y)(x + 2) = '-'
                        theMap(y)(x + 3) = '#'
                        mapRoom(currentRoom.exits(East), y, x + 3, distance + 1)
                    case West =>
                        theMap(y)(x - 1) = '-'
                        theMap(y)(x - 2) = '-'
                        theMap(y)(x - 3) = '#'
                        mapRoom(currentRoom.exits(West), y, x - 3, distance + 1)
                }
            }
        }

        theMap(2 * range)(3 * range) = 'X'
        mapRoom(room, 2 * range, 3 * range, 0)
        theMap.map(_.mkString).toList
    }
}
