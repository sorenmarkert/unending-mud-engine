package core

import core.Direction.{East, North, South, West}

import scala.Array.tabulate
import scala.collection.mutable.{Set => MSet}
import scala.math.{abs, max, min}

object MiniMap {

    def miniMap(room: Room, range: Int) = {

        val traversedRooms = MSet[Room]()

        def mapToGraph(currentRoom: Room, x: Int, y: Int, currentRange: Int): MapNode = {

            traversedRooms += currentRoom

            val unvisitedExits =
                if (currentRange < range)
                    currentRoom.exits.filterNot { case (_, v) => traversedRooms contains v.toRoom }.toMap
                else
                    Map.empty[Direction.Value, Exit]

            MapNode(
                unvisitedExits.get(North) map (exit => mapToGraph(exit.toRoom, x, y - exit.distance, currentRange + 1)),
                unvisitedExits.get(South) map (exit => mapToGraph(exit.toRoom, x, y + exit.distance, currentRange + 1)),
                unvisitedExits.get(East) map (exit => mapToGraph(exit.toRoom, x + exit.distance, y, currentRange + 1)),
                unvisitedExits.get(West) map (exit => mapToGraph(exit.toRoom, x - exit.distance, y, currentRange + 1)),
                currentRoom, x, y)
        }

        def maxCoordinates(mapNode: MapNode): (Int, Int) =
            Vector(mapNode.north map maxCoordinates,
                mapNode.south map maxCoordinates,
                mapNode.east map maxCoordinates,
                mapNode.west map maxCoordinates)
                .flatten
                .fold(abs(mapNode.x), abs(mapNode.y))((pairA, pairB) => (max(pairA._1, pairB._1), max(pairA._2, pairB._2)))

        val graphMap = mapToGraph(room, 0, 0, 0)
        val maxPair = maxCoordinates(graphMap)
        val (maxX, maxY) = (max(2, maxPair._1), max(2, maxPair._2))
        val (centerX, centerY) = (3 * maxX, 2 * maxY)
        val theMap = tabulate(4 * maxY + 1, 6 * maxX + 1)((_, _) => ' ')

        def drawMap(mapNode: MapNode): Unit = {
            val (fromX, fromY) = (centerX + 3 * mapNode.x, centerY + 2 * mapNode.y)

            Vector(mapNode.north, mapNode.south, mapNode.east, mapNode.west)
                .flatten
                .foreach { currentMapNode =>
                    drawMap(currentMapNode)
                    val (toX, toY) = (centerX + 3 * currentMapNode.x, centerY + 2 * currentMapNode.y)
                    if (fromX == toX) {
                        min(fromY, toY) until max(fromY, toY) foreach (theMap(_)(toX) = '|')
                    } else {
                        min(fromX, toX) until max(fromX, toX) foreach (theMap(toY)(_) = '-')
                    }
                    theMap(toY)(toX) = '#'
                }
        }

        drawMap(graphMap)
        theMap(centerY)(centerX) = 'X'
        theMap.map(_.mkString).toList
    }

    def frameMap(map: List[String]) = {
        val line = Array.tabulate(map(0).size)((_) => '-').mkString
        val header = "/" + line + "\\"
        val footer = "\\" + line + "/"

        header :: (map map ("|" + _ + "|")) appended footer
    }
}

case class MapNode(north: Option[MapNode], south: Option[MapNode],
                   east: Option[MapNode], west: Option[MapNode],
                   room: Room, x: Int, y: Int)
