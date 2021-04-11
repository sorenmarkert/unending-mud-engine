package core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors._
import core.GlobalState.{Closing, runState}
import core.commands.{Command, InstantCommand, TimedCommand}
import play.api.Logger

import scala.collection.mutable.{ListBuffer, Map => MMap, Set => MSet, SortedMap => MSortedMap}
import scala.concurrent.duration.DurationInt

object StateActor {

    sealed trait StateActorMessage

    case object Tick extends StateActorMessage

    case class CommandExecution(command: Command, character: Character, argument: List[String]) extends StateActorMessage

    case class Interrupt(character: Character) extends StateActorMessage


    private val logger = Logger(this.getClass)

    private val tickInterval = 100.milliseconds
    private var tickCounter = 0
    private val commandQueue = ListBuffer.empty[CommandExecution]
    private val timedCommandsWaitingMap = MSortedMap.empty[Int, Vector[CommandExecution]]
    private val charactersInActionMap = MMap.empty[Character, Int]


    def apply(): Behavior[StateActorMessage] =
        setup { context =>
            withTimers { timers =>

                logger.warn("Starting state actor with tick interval " + tickInterval)
                timers.startTimerAtFixedRate(Tick, tickInterval)
                handleMessage(context)
            }
        }

    private def handleMessage(context: ActorContext[StateActorMessage]): Behavior[StateActorMessage] =
        receiveMessage {
            case command: CommandExecution =>
                logger.warn("Received command: " + command.argument.mkString(" "))
                commandQueue append command
                same
            case Interrupt(character) =>
                charactersInActionMap remove character foreach { tick =>
                    timedCommandsWaitingMap(tick) = timedCommandsWaitingMap(tick) filterNot (_.character == character)
                }
                same
            case Tick =>
                executeQueuedCommands()
                tickCounter += 1
                if (runState == Closing) stopped
                else same
        }

    private def executeQueuedCommands() = {

        val charactersWhoReceivedMessages = MSet.empty[Character]

        timedCommandsWaitingMap get tickCounter foreach {
            _ foreach {
                case CommandExecution(TimedCommand(_, _, endFunc), character, argument) =>
                    endFunc(character, argument)
                    charactersWhoReceivedMessages addOne character
                case _ =>
            }
        }
        timedCommandsWaitingMap remove tickCounter

        commandQueue foreach {
            // TODO: check if need to block if waiting for a TimedCommand
            case CommandExecution(InstantCommand(func, canInterrupt), character, argument) =>
                func(character, argument)
                charactersWhoReceivedMessages addOne character
            case commandExecution @ CommandExecution(TimedCommand(duration, beginFunc, _), character, argument) =>
                beginFunc(character, argument) match {
                    case Some(_) => // TODO: error case
                    case None =>
                        val executeAtTick = tickCounter + (duration / tickInterval).toInt
                        val commandsEndingAtTick = timedCommandsWaitingMap.getOrElse(executeAtTick, Vector.empty[CommandExecution])
                        timedCommandsWaitingMap(executeAtTick) = commandsEndingAtTick appended commandExecution
                        charactersInActionMap(character) = executeAtTick
                        charactersWhoReceivedMessages addOne character
                }
        }
        commandQueue.clear()

        // TODO:        charactersWhoActed foreach (_.sendPrompt)
    }
}
