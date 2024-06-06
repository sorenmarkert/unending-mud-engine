package core.connection

import akka.actor.typed.ActorSystem
import akka.event.slf4j.SLF4JLogging
import core.commands.Commands
import core.gameunit.PlayerCharacter
import core.state.{GlobalState, StateActor}
import core.storage.Storage
import org.eclipse.jetty.websocket.api.Session

import scala.concurrent.ExecutionContext.Implicits.global

class WebSocketEndPoint(private val name: String)(using globalState: GlobalState, commands: Commands, storage: Storage)(using ActorSystem[StateActor.Message])
    extends Session.Listener.AbstractAutoDemanding with SLF4JLogging:

    private var session: Session = null
    private var playerCharacter: PlayerCharacter = null

    override def onWebSocketOpen(session: Session) =
        this.session = session
        log.info(s"Connection from $name@${session.getRemoteSocketAddress}")
        val connection = WebSocketConnection(session)
        storage.loadPlayer(name, connection)
            .map {
                case Some(pc) =>
                    log.info(s"Loaded player $name")
                    pc
                case None =>
                    val startingRoom = globalState.rooms("roomCenter")
                    log.info(s"Created player $name")
                    startingRoom.createPlayerCharacter(name, connection)
            }
            .foreach(playerCharacter = _)

    override def onWebSocketText(input: String) =
        commands.executeCommandAtNextTick(playerCharacter, input)

    override def onWebSocketError(cause: Throwable) =
        cause.getClass.getSimpleName match
            case "WebSocketTimeoutException" =>
                log.info(s"Connection timeout $name@${session.getRemoteSocketAddress}: ${cause.getMessage}")
            case _ =>
                log.error("WebSocket Error", cause)

    override def onWebSocketClose(statusCode: Int, reason: String) =
        storage.savePlayer(playerCharacter)
        playerCharacter.destroy
        log.info(s"Connection closed ${playerCharacter.name}@${session.getRemoteSocketAddress}, statusCode: $statusCode, reason: $reason")
