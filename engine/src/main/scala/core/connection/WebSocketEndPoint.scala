package core.connection

import akka.event.slf4j.SLF4JLogging
import core.commands.Commands
import core.gameunit.PlayerCharacter
import core.state.GlobalState
import core.storage.Storage
import org.eclipse.jetty.websocket.api.Session

class WebSocketEndPoint(private val name: String)(using globalState: GlobalState, commands: Commands, storage: Storage)
    extends Session.Listener.AbstractAutoDemanding with SLF4JLogging:

    import globalState.*

    private var session: Session = null
    private var playerCharacter: PlayerCharacter = null

    override def onWebSocketOpen(session: Session) =
        this.session = session
        log.info(s"Connection from $name@${session.getRemoteSocketAddress}")
        storage.loadPlayer(name, WebSocketConnection(session), playerCharacter = _)

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
