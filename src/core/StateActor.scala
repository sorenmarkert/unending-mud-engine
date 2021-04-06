package core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.{receiveMessage, setup, withTimers}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import core.commands.{Command, DurationCommand, InstantCommand}
import play.api.Logger

import scala.collection.mutable.{ListBuffer, Map => MMap, Set => MSet, SortedMap => MSortedMap}
import scala.concurrent.duration.DurationInt

object StateActor {

    sealed trait StateActorMessage

    case object Tick extends StateActorMessage

    case class CommandExecution(command: Command, character: Character, argument: List[String]) extends StateActorMessage

    case class Interrupt(character: Character) extends StateActorMessage


    private val logger = Logger(this.getClass)

    private var tickCounter = 0
    private val commandQueue = ListBuffer.empty[CommandExecution]
    private val durationCommandsWaitingMap = MSortedMap.empty[Int, ListBuffer[CommandExecution]].withDefaultValue(ListBuffer())
    private val charactersInActionMap = MMap.empty[Character, Int]


    def apply(): Behavior[StateActorMessage] =
        setup { context =>
            withTimers { timers =>

                val tickInterval = 100.milliseconds
                logger.warn("Starting state actor with tick interval " + tickInterval)
                timers.startTimerAtFixedRate(Tick, tickInterval)
                handleMessage(context)
            }
        }

    private def handleMessage(context: ActorContext[StateActorMessage]): Behavior[StateActorMessage] =
        receiveMessage {
            case command: CommandExecution =>
                commandQueue append command
                Behaviors.same
            case Interrupt(character) =>
                charactersInActionMap remove character foreach {
                    durationCommandsWaitingMap(_) filterInPlace (_.character != character)
                }
                Behaviors.same
            case Tick =>
                executeQueuedCommands()
                tickCounter += 1
                Behaviors.same
        }

    private def executeQueuedCommands() = {

        val charactersWhoActed = MSet.empty[Character]

        durationCommandsWaitingMap(tickCounter) foreach {
            case CommandExecution(DurationCommand(_, _, endFunc), character, argument) =>
                endFunc(character, argument)
                charactersWhoActed addOne character
            case _ =>
        }
        durationCommandsWaitingMap remove tickCounter

        commandQueue foreach {
            // TODO: check if need to block if waiting for a DurationCommand
            case CommandExecution(InstantCommand(func), character, argument) =>
                func(character, argument)
                charactersWhoActed addOne character
            case commandExecution@CommandExecution(DurationCommand(duration, beginFunc, _), character, argument) =>
                beginFunc(character, argument) match {
                    case Some(_) => // TODO: error case
                    case None =>
                        val executeAtTick = tickCounter + duration
                        durationCommandsWaitingMap(executeAtTick) append commandExecution
                        charactersInActionMap(character) = executeAtTick
                        charactersWhoActed addOne character
                }
        }
        commandQueue.clear()

        // TODO:        charactersWhoActed foreach (_.sendPrompt)
    }
}
