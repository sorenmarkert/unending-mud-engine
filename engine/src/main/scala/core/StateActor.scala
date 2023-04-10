package core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors.*
import akka.event.slf4j.{Logger, SLF4JLogging}
import core.RunState.Closing
import core.commands.*

import scala.collection.mutable.{ListBuffer, Map as MMap, Set as MSet, SortedMap as MSortedMap}
import scala.concurrent.duration.DurationInt

object StateActor extends SLF4JLogging:

    sealed trait StateActorMessage


    case object Tick extends StateActorMessage


    case class CommandExecution(command: Command, character: gameunit.Character, argument: List[String]) extends StateActorMessage


    case class Interrupt(character: gameunit.Character) extends StateActorMessage


    private val tickInterval            = 100.milliseconds // TODO: make configurable
    private var tickCounter             = 0
    private val commandQueue            = ListBuffer.empty[CommandExecution]
    private val timedCommandsWaitingMap = MSortedMap.empty[Int, Vector[CommandExecution]]
    private val charactersInActionMap   = MMap.empty[gameunit.Character, Int]

    def apply(): Behavior[StateActorMessage] =
        setup { context =>
            withTimers { timers =>

                log.info("Starting state actor with tick interval " + tickInterval)
                timers.startTimerAtFixedRate(Tick, tickInterval)
                handleMessage(context)
            }
        }

    private def handleMessage(context: ActorContext[StateActorMessage])(using globalState: GlobalState): Behavior[StateActorMessage] =
        receiveMessage {
            case command@CommandExecution(_, _, argument) =>
                log.info("Received command: " + argument.mkString(" "))
                commandQueue append command
                same
            case Interrupt(character)                     =>
                charactersInActionMap remove character foreach { tick =>
                    timedCommandsWaitingMap(tick) = timedCommandsWaitingMap(tick) filterNot (_.character == character)
                }
                same
            case Tick                                     =>
                executeQueuedCommands()
                tickCounter += 1
                if tickCounter == Int.MaxValue then tickCounter = 0
                if globalState.runState == Closing then stopped
                else same
        }

    private def executeQueuedCommands() = {

        val charactersWhoReceivedMessages = MSet.empty[gameunit.Character]

        timedCommandsWaitingMap get tickCounter foreach {
            _ foreach {
                case CommandExecution(TimedCommand(_, _, endFunc), character, argument) =>
                    endFunc(character, argument)
                    charactersWhoReceivedMessages addOne character
                case _                                                                  =>
            }
        }
        timedCommandsWaitingMap remove tickCounter

        commandQueue foreach {
            // TODO: check if need to block if waiting for a TimedCommand
            case CommandExecution(InstantCommand(func, canInterrupt), character, argument)                    =>
                func(character, argument)
                charactersWhoReceivedMessages addOne character
            case commandExecution@CommandExecution(TimedCommand(duration, beginFunc, _), character, argument) =>
                beginFunc(character, argument) match {
                    case Some(_) => // TODO: error case
                    case None    =>
                        val executeAtTick        = tickCounter + (duration / tickInterval).toInt
                        val commandsEndingAtTick = timedCommandsWaitingMap.getOrElse(executeAtTick, Vector.empty[CommandExecution])
                        timedCommandsWaitingMap(executeAtTick) = commandsEndingAtTick appended commandExecution
                        charactersInActionMap(character) = executeAtTick
                        charactersWhoReceivedMessages addOne character
                }
        }
        commandQueue.clear()

        // TODO:        charactersWhoActed foreach (_.sendPrompt)
    }
