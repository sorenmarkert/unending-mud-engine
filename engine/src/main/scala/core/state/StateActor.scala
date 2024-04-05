package core.state

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.*
import akka.event.slf4j.SLF4JLogging
import core.*
import core.commands.*
import core.commands.Commands.{Command, InstantCommand, TimedCommand}
import core.gameunit.{Mobile, PlayerCharacter}
import core.state.RunState.Closing

import scala.collection.mutable.{ListBuffer, Map as MMap, SortedMap as MSortedMap}
import scala.concurrent.duration.DurationInt


sealed trait StateActorMessage

case class CommandExecution(command: Command, character: gameunit.Mobile, argument: List[String]) extends StateActorMessage

case class Interrupt(character: gameunit.Mobile) extends StateActorMessage

case object Tick extends StateActorMessage


object StateActor extends SLF4JLogging:

    private val tickInterval = 100.milliseconds
    private var tickCounter = 0
    private val commandQueue = ListBuffer.empty[CommandExecution]
    private val timedCommandsWaitingMap = MSortedMap.empty[Int, ListBuffer[CommandExecution]]
    private val charactersInActionMap = MMap.empty[gameunit.Mobile, Int]

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
                commandQueue append commandExecution
                same
            case Interrupt(character) =>
                charactersInActionMap remove character foreach { tick =>
                    timedCommandsWaitingMap(tick) = timedCommandsWaitingMap(tick) filterNot (_.character == character)
                }
                same
            case Tick =>
                executeCommands()
                tickCounter += 1
                if globalState.runState == Closing then stopped
                else same
        }

    private def executeCommands()(using messageSender: MessageSender): Unit = {
        val recipientsOfTimed = executeEndOfTimedCommands()
        val (recipientsOfQueued, recipientsWhoNeedMiniMap) = executeQueuedCommands()
        sendQueuedMessagesAndAddPrompts(recipientsOfTimed ++ recipientsOfQueued, recipientsWhoNeedMiniMap)
    }

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

    private def executeQueuedCommands(): (Set[PlayerCharacter], Set[Mobile]) = {
        val (playersWhoReceivedMessages, recipientsWhoNeedMiniMap) = commandQueue.map {
            case CommandExecution(InstantCommand(func, canInterrupt), character, argument) =>
                val commandResult = func(character, argument)
                (commandResult.playersWhoReceivedMessages, if commandResult.addMiniMap then Seq(character) else Seq())
            case commandExecution@CommandExecution(TimedCommand(duration, beginFunc, _), character, argument) =>
                val commandResult = beginFunc(character, argument)
                val tickToExecuteAt = tickCounter + (duration / tickInterval).toInt
                timedCommandsWaitingMap.getOrElse(tickToExecuteAt, ListBuffer.empty).append(commandExecution)
                charactersInActionMap(character) = tickToExecuteAt
                (commandResult.playersWhoReceivedMessages, if commandResult.addMiniMap then Seq(character) else Seq())
        }.unzip
        commandQueue.clear()
        (playersWhoReceivedMessages.flatten.toSet, recipientsWhoNeedMiniMap.flatten.toSet)
    }

    private def sendQueuedMessagesAndAddPrompts(playersWhoReceivedMessages: Set[PlayerCharacter],
                                                recipientsWhoNeedMiniMap: Set[Mobile])
                                               (using messageSender: MessageSender): Unit =
        playersWhoReceivedMessages.foreach {
            playerCharacter =>
                messageSender.sendAllEnqueuedMessages(
                    playerCharacter,
                    addMiniMap = recipientsWhoNeedMiniMap.contains(playerCharacter))
        }
