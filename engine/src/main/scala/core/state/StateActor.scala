package core.state

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.*
import akka.event.slf4j.SLF4JLogging
import core.*
import core.commands.*
import core.commands.Commands.{Command, InstantCommand, TimedCommand}
import core.gameunit.{Mobile, PlayerCharacter}
import core.state.RunState.Closing

import java.util.UUID
import scala.collection.mutable.{LinkedHashMap, ListBuffer, Map as MMap, SortedMap as MSortedMap}
import scala.concurrent.duration.DurationInt


sealed trait StateActorMessage

case object Tick extends StateActorMessage

case class CommandExecution(command: Command, character: Mobile, argument: List[String]) extends StateActorMessage

case class Interrupt(character: Mobile) extends StateActorMessage

case class Destroy(character: Mobile) extends StateActorMessage


object StateActor extends SLF4JLogging:

    private val tickInterval = 100.milliseconds
    private var tickCounter = 0
    private val commandQueue: MMap[UUID, ListBuffer[CommandExecution]] = LinkedHashMap()
    private val timedCommandsWaitingMap = MSortedMap.empty[Int, ListBuffer[CommandExecution]]
    private val charactersInActionMap = MMap.empty[Mobile, Int]

    def apply(): Behavior[StateActorMessage] =
        setup { context =>
            withTimers { timers =>
                log.info("Starting state actor with tick interval " + tickInterval)
                timers.startTimerAtFixedRate(Tick, tickInterval)
                handleActorMessage()
            }
        }

    private def handleActorMessage()(using globalState: GlobalState, messageSender: MessageSender): Behavior[StateActorMessage] =
        receiveMessage {
            case commandExecution: CommandExecution =>
                log.info("Received command: " + commandExecution.argument.mkString(" "))
                commandQueue.getOrElseUpdate(commandExecution.character.uuid, ListBuffer()).append(commandExecution)
                same
            case Interrupt(character) =>
                charactersInActionMap remove character foreach { tick =>
                    timedCommandsWaitingMap(tick) = timedCommandsWaitingMap(tick) filterNot (_.character == character)
                }
                same
            case Destroy(character) =>
                commandQueue remove character.uuid
                charactersInActionMap remove character
                timedCommandsWaitingMap.foreach {
                    case (_, commandExecutions) =>
                        commandExecutions
                            .filter { case CommandExecution(_, char, _) => char == character}
                            .foreach(commandExecutions.subtractOne)
                }
                same
            case Tick =>
                executeCommands()
                tickCounter += 1
                if globalState.runState == Closing then stopped
                else same
        }

    private def executeCommands(): Unit =
        val recipientsOfTimed = executeEndOfTimedCommands()
        val (recipientsOfQueued, recipientsWhoNeedMiniMap) = executeQueuedCommands()
        sendQueuedMessagesAndAddPrompts(recipientsOfTimed ++ recipientsOfQueued, recipientsWhoNeedMiniMap)

    private def executeEndOfTimedCommands() =
        val playersWhoReceivedMessages = timedCommandsWaitingMap.get(tickCounter).toSet
            .flatMap {
                _.flatMap {
                    case CommandExecution(TimedCommand(_, _, endFunc), character, argument) =>
                        val commandResult = endFunc(character, argument)
                        commandResult.playersWhoReceivedMessages
                    case _ => Seq()
                }
            }
        timedCommandsWaitingMap remove tickCounter
        playersWhoReceivedMessages

    private def executeQueuedCommands(): (Set[PlayerCharacter], Set[Mobile]) =
        val (playersWhoReceivedMessages, recipientsWhoNeedMiniMap) = commandQueue.map {
            case (characterUuid, commandExecutions) =>
                val (commandResult, character) = commandExecutions.head match
                    case CommandExecution(InstantCommand(func, canInterrupt), character, argument) =>
                        (func(character, argument), character)
                    case commandExecution@CommandExecution(TimedCommand(duration, beginFunc, _), character, argument) =>
                        val tickToExecuteEndFuncAt = tickCounter + (duration / tickInterval).toInt
                        timedCommandsWaitingMap.getOrElse(tickToExecuteEndFuncAt, ListBuffer()).append(commandExecution)
                        charactersInActionMap(character) = tickToExecuteEndFuncAt
                        (beginFunc(character, argument), character)
                commandExecutions.remove(0)
                if commandExecutions.isEmpty then
                    commandQueue.remove(characterUuid)
                (commandResult.playersWhoReceivedMessages, if commandResult.addMiniMap then Seq(character) else Seq())
        }.unzip

        (playersWhoReceivedMessages.flatten.toSet, recipientsWhoNeedMiniMap.flatten.toSet)

    private def sendQueuedMessagesAndAddPrompts(playersWhoReceivedMessages: Set[PlayerCharacter],
                                                recipientsWhoNeedMiniMap: Set[Mobile])
                                               (using messageSender: MessageSender): Unit =
        playersWhoReceivedMessages.foreach {
            playerCharacter =>
                messageSender.sendAllEnqueuedMessages(
                    playerCharacter,
                    addMiniMap = recipientsWhoNeedMiniMap.contains(playerCharacter))
        }
