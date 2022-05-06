package dto

import java.util.UUID

sealed trait GameUnitDTO(uuid: UUID, name: String, description: String):
    override def toString: String = s"${this.getClass.getSimpleName}($uuid, $name, $description)"


abstract class CharacterDTO(uuid: UUID, name: String, description: String)
    extends GameUnitDTO(uuid: UUID, name: String, description: String)


case class PlayerCharacterDTO(uuid: UUID, name: String, description: String)
    extends CharacterDTO(uuid: UUID, name: String, description: String)


case class NonPlayerCharacterDTO(uuid: UUID, name: String, description: String)
    extends CharacterDTO(uuid: UUID, name: String, description: String)


case class ItemDTO(uuid: UUID, name: String, description: String)
    extends GameUnitDTO(uuid: UUID, name: String, description: String)


case class RoomDTO(uuid: UUID, name: String, description: String, exits: Map[Direction, String])
    extends GameUnitDTO(uuid: UUID, name: String, description: String)


enum Direction(title: String):
    case North extends Direction("north")
    case South extends Direction("south")
    case East extends Direction("east")
    case West extends Direction("west")
    case Up extends Direction("up")
    case Down extends Direction("down")
