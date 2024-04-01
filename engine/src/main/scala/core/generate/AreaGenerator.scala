package core.generate

import core.gameunit
import core.gameunit.*
import core.gameunit.Direction.*

import scala.annotation.tailrec
import scala.util.Random

class AreaGenerator()(using random: Random, globalState: GlobalState):

    private type Dir = (Int, Int)
    private type Pos = (Int, Int)

    private val directions: Map[Dir, Direction] = Map(
        (0, 1) -> North,
        (0, -1) -> South,
        (1, 0) -> East,
        (-1, 0) -> West)

    def generate(maxHallways: Int, maxLength: Int) =

        val board = generateBoard(Set((0, 0)), (0, 0), (random shuffle directions.keySet).head, maxHallways, maxLength)

        val roomsByPosition = (board map (p => p -> Room(s"generated[${p._1},${p._2}]", "This is a generated room"))).toMap

        roomsByPosition foreach { case (position, room) =>
            getAdjacentRooms(position, roomsByPosition)
                .foreach { case (adjacentRoom, direction) =>
                    room.addLink(direction, adjacentRoom)
                }
        }

        roomsByPosition


    @tailrec
    private def generateBoard(board: Set[Pos], position: Pos, lastDirection: Dir, hallways: Int, maxLength: Int): Set[Pos] =
        if hallways > 0 then
            val leftAndRight = directions.keySet - lastDirection - lastDirection.opposite
            val newDirection = (random shuffle leftAndRight).head
            val length = random.between(1, maxLength + 1)
            val newPosition = position plus (newDirection mult length)
            val newHallway = position.fill(newDirection, length)
            generateBoard(board ++ newHallway, newPosition, newDirection, hallways - 1, maxLength)
        else
            board


    private def getAdjacentRooms(position: Pos, roomsByPosition: Map[(Int, Int), Room]) =
        directions flatMap {
            case (dir, direction) =>
                roomsByPosition get (position plus dir) map ((_, direction))
        }


    extension (thisOne: (Int, Int))

        private def opposite = (thisOne._1 * -1, thisOne._2 * -1)

        infix private def plus(other: Dir) = (thisOne._1 + other._1, thisOne._2 + other._2)

        infix private def mult(length: Int) = (thisOne._1 * length, thisOne._2 * length)

        private def fill(direction: Dir, length: Int) =
            (1 to length).foldLeft(List[Dir]())((a, b) => (thisOne plus (direction mult b)) :: a)
