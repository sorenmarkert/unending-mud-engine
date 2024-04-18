package core

import core.gameunit.{Direction, Room}

import scala.annotation.tailrec

object PathFinding:

    def findPath(origin: Room, destination: Room): Either[String, Seq[Direction]] =

        @tailrec
        def getRoomsWithDirectionToOrigin(previousRoomsWithDirectionToOrigin: Map[Room, Direction]): Map[Room, Direction] =
            val visitedRooms = previousRoomsWithDirectionToOrigin.keySet
            val newRoomsWithDirectionToOrigin = visitedRooms
                .flatMap(_.exits.map { case (direction, exit) => exit.toRoom -> direction.opposite })
                .toMap
            val nextRoomsWithDirectionToOrigin = newRoomsWithDirectionToOrigin ++ previousRoomsWithDirectionToOrigin

            if newRoomsWithDirectionToOrigin.isDefinedAt(destination) || nextRoomsWithDirectionToOrigin.size == previousRoomsWithDirectionToOrigin.size then
                nextRoomsWithDirectionToOrigin
            else
                getRoomsWithDirectionToOrigin(nextRoomsWithDirectionToOrigin)

        if origin == destination then
            Left("You're already there.")
        else
            val roomsWithDirectionToOrigin = getRoomsWithDirectionToOrigin(Map(origin -> null))

            def backtrackToOrigin(to: Room): List[Direction] =
                if to == origin then
                    Nil
                else
                    val directionToOrigin = roomsWithDirectionToOrigin(to)
                    val previousRoom = to.exits(directionToOrigin).toRoom
                    directionToOrigin.opposite :: backtrackToOrigin(previousRoom)

            roomsWithDirectionToOrigin.get(destination)
                .map(_ => backtrackToOrigin(destination).reverse)
                .toRight("No path found to the destination.")
            