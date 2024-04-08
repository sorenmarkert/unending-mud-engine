package core.login

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors.*
import akka.event.slf4j.SLF4JLogging
import core.*
import core.state.GlobalState
import core.storage.Storage


sealed trait LoginActorMessage

case object GenerateName extends LoginActorMessage

case class CheckName(name: String) extends LoginActorMessage

case class CheckPassword(name: String, password: String) extends LoginActorMessage


object LoginActor extends SLF4JLogging:

    def apply()(using storage: Storage, messageSender: MessageSender): Behavior[LoginActorMessage] =
        setup { context =>
            context.system.receptionist
            log.info("Starting login actor")
            handleActorMessage()
        }

    private def handleActorMessage()(using globalState: GlobalState, messageSender: MessageSender): Behavior[LoginActorMessage] =
        receiveMessage {
            case GenerateName =>
                same
            case CheckName(name) =>
                log.info("Received name to check: " + name)
                same
        }
