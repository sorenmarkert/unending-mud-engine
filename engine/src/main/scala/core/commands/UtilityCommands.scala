package core.commands

import core.MessageSender
import core.PathFinding.findPath
import core.commands.Commands.CommandResult
import core.gameunit.Mobile
import core.state.GlobalState

class UtilityCommands()(using messageSender: MessageSender, globalState: GlobalState):

    import messageSender.*

    private[commands] def path(character: Mobile, commandWords: Seq[String]): CommandResult =
        val message = commandWords match
            case _ :: Nil => "Find a path to which room?"
            case _ :: roomId :: _ =>
                globalState.rooms.get(commandWords.tail.head) match
                    case None => "No such room id."
                    case Some(room) => findPath(character.outside, room) match
                        case Left(msg) => msg
                        case Right(path) => path.map(_.display).mkString(", ")
        CommandResult(sendMessageToCharacter(character, message))


object UtilityCommands:
    given UtilityCommands = UtilityCommands()
