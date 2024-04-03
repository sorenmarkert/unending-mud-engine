package core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.*
import akka.event.slf4j.SLF4JLogging
import core.commands.*
import core.commands.Commands.{Command, InstantCommand, TimedCommand}
import core.gameunit.RunState.Closing
import core.gameunit.{GlobalState, Mobile, PlayerCharacter}

import scala.collection.mutable.{ListBuffer, Map as MMap, Set as MSet, SortedMap as MSortedMap}
import scala.concurrent.duration.DurationInt


sealed trait StateActorMessage

case object Tick extends StateActorMessage

case class CommandExecution(command: Command, character: gameunit.Mobile, argument: List[String]) extends StateActorMessage

case class Interrupt(character: gameunit.Mobile) extends StateActorMessage


// TODO: unit test
object StateActor extends SLF4JLogging:

    private val tickInterval = 100.milliseconds // TODO: make configurable
    private var tickCounter = 0
    private val commandQueue = ListBuffer.empty[CommandExecution]
    private val timedCommandsWaitingMap = MSortedMap.empty[Int, Vector[CommandExecution]]
    private val charactersInActionMap = MMap.empty[gameunit.Mobile, Int]

    def apply(): Behavior[StateActorMessage] =
        setup { context =>
            withTimers { timers =>

                log.info("Starting state actor with tick interval " + tickInterval)
                timers.startTimerAtFixedRate(Tick, tickInterval)
                handleActorMessage(context)
            }
        }

    private def handleActorMessage(context: ActorContext[StateActorMessage])(using globalState: GlobalState, messageSender: MessageSender): Behavior[StateActorMessage] =
        receiveMessage {
            case commandExecution: CommandExecution =>
                // TODO: one command per character per tick
                log.info("Received command: " + commandExecution.argument.mkString(" "))
                commandQueue append commandExecution
                same
            case Interrupt(character) =>
                charactersInActionMap remove character foreach { tick =>
                    timedCommandsWaitingMap(tick) = timedCommandsWaitingMap(tick) filterNot (_.character == character)
                }
                same
            case Tick =>
                executeQueuedCommands()
                tickCounter += 1
                if globalState.runState == Closing then stopped
                else same
        }

    private def executeQueuedCommands()(using messageSender: MessageSender) = {

        val playerCharactersWhoReceivedMessages = MSet.empty[PlayerCharacter]
        val playerCharactersWhoNeedMiniMap = MSet.empty[Mobile]

        timedCommandsWaitingMap get tickCounter foreach {
            _ foreach {
                case CommandExecution(TimedCommand(_, _, endFunc), character, argument) =>
                    val playerCharacters = endFunc(character, argument)
                    playerCharactersWhoReceivedMessages.addAll(playerCharacters)
                case _ =>
            }
        }
        timedCommandsWaitingMap remove tickCounter

        commandQueue foreach {
            // TODO: check if need to block if waiting for a TimedCommand
            case CommandExecution(InstantCommand(func, canInterrupt, addMiniMap), character, argument) =>
                val playerCharacters = func(character, argument)
                playerCharactersWhoReceivedMessages.addAll(playerCharacters)
                if addMiniMap then playerCharactersWhoNeedMiniMap.add(character)
            case commandExecution@CommandExecution(TimedCommand(duration, beginFunc, _), character, argument) =>
                val playerCharacters = beginFunc(character, argument)
                playerCharactersWhoReceivedMessages.addAll(playerCharacters)
                val tickToExecuteAt = tickCounter + (duration / tickInterval).toInt
                val commandsEndingAtTick = timedCommandsWaitingMap.getOrElse(tickToExecuteAt, Vector.empty)
                timedCommandsWaitingMap(tickToExecuteAt) = commandsEndingAtTick appended commandExecution
                charactersInActionMap(character) = tickToExecuteAt
        }
        commandQueue.clear()

        playerCharactersWhoReceivedMessages.foreach {
            playerCharacter =>
                messageSender.sendAllEnqueuedMessages(
                    playerCharacter,
                    addMiniMap = playerCharactersWhoNeedMiniMap.contains(playerCharacter))
        }
    }
