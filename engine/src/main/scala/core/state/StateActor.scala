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


object StateActor extends SLF4JLogging:

    sealed trait Message

    case object Tick extends Message

    case class Execute(command: Command, character: Mobile, argument: List[String]) extends Message

    case class Interrupt(character: Mobile) extends Message

    case class Destroy(character: Mobile) extends Message


    private val tickInterval = 100.milliseconds
    private var tickCounter = 0
    private val commandQueue: MMap[UUID, ListBuffer[Execute]] = LinkedHashMap()
    private val timedCommandsOngoingMap = MSortedMap.empty[Int, ListBuffer[Execute]]
    private val charactersInActionMap = MMap.empty[Mobile, Int] // TODO: use UUID as key, or override .equals

    def apply(): Behavior[Message] =
        setup { context =>
            withTimers { timers =>
                log.info("Starting state actor with tick interval " + tickInterval)
                timers.startTimerAtFixedRate(Tick, tickInterval)
                handleActorMessage()
            }
        }

    private def handleActorMessage()(using globalState: GlobalState, messageSender: MessageSender): Behavior[Message] =
        receiveMessage {
            case execute: Execute =>
                log.info("Received command: " + execute.argument.mkString(" "))
                commandQueue.getOrElseUpdate(execute.character.uuid, ListBuffer()).append(execute)
                same
            case Interrupt(character) =>
                removeOngoingTimedCommand(character)
                same
            case Destroy(character) =>
                commandQueue.remove(character.uuid)
                removeOngoingTimedCommand(character)
                same
            case Tick =>
                executeCommands()
                tickCounter += 1
                if globalState.runState == Closing then stopped
                else same
        }

    private def removeOngoingTimedCommand(character: Mobile): Unit = {
        charactersInActionMap.remove(character)
            .foreach { tickCount =>
                val updatedExecutes = timedCommandsOngoingMap(tickCount) filterNot (_.character == character)
                timedCommandsOngoingMap(tickCount) = updatedExecutes
                if updatedExecutes.isEmpty then
                    timedCommandsOngoingMap.remove(tickCount)
            }
    }

    private def executeCommands(): Unit =
        val recipientsOfTimed = executeEndOfTimedCommands()
        val (recipientsOfQueued, recipientsWhoNeedMiniMap) = executeQueuedCommands()
        sendQueuedMessages(recipientsOfTimed ++ recipientsOfQueued, recipientsWhoNeedMiniMap)

    private def executeEndOfTimedCommands() =
        val playersWhoReceivedMessages = timedCommandsOngoingMap.get(tickCounter).toSet
            .flatMap {
                _.flatMap {
                    case Execute(TimedCommand(_, _, endFunc), character, argument) =>
                        val commandResult = endFunc(character, argument)
                        commandResult.playersWhoReceivedMessages
                    case _ => Seq()
                }
            }
        timedCommandsOngoingMap.remove(tickCounter)
        playersWhoReceivedMessages

    private def executeQueuedCommands(): (Set[PlayerCharacter], Set[Mobile]) =
        val (playersWhoReceivedMessages, recipientsWhoNeedMiniMap) = commandQueue.map {
            case (characterUuid, executes) =>
                val (commandResult, character) = executes.head match
                    case Execute(InstantCommand(func, canInterrupt), character, argument) =>
                        (func(character, argument), character)
                    case execute@Execute(TimedCommand(duration, beginFunc, _), character, argument) =>
                        val tickToExecuteEndFuncAt = tickCounter + (duration / tickInterval).toInt
                        timedCommandsOngoingMap.getOrElse(tickToExecuteEndFuncAt, ListBuffer()).append(execute)
                        charactersInActionMap(character) = tickToExecuteEndFuncAt
                        (beginFunc(character, argument), character)
                executes.remove(0)
                if executes.isEmpty then
                    commandQueue.remove(characterUuid)
                (commandResult.playersWhoReceivedMessages, if commandResult.addMiniMap then Seq(character) else Seq())
        }.unzip

        (playersWhoReceivedMessages.flatten.toSet, recipientsWhoNeedMiniMap.flatten.toSet)

    private def sendQueuedMessages(playersWhoReceivedMessages: Set[PlayerCharacter],
                                   recipientsWhoNeedMiniMap: Set[Mobile])
                                  (using messageSender: MessageSender): Unit =
        playersWhoReceivedMessages.foreach {
            playerCharacter =>
                messageSender.sendAllEnqueuedMessages(
                    playerCharacter,
                    addMiniMap = recipientsWhoNeedMiniMap.contains(playerCharacter))
        }
