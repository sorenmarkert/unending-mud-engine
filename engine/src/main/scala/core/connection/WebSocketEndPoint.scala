package core.connection

import akka.event.slf4j.SLF4JLogging
import core.commands.Commands
import core.gameunit.{GlobalState, PlayerCharacter}
import org.eclipse.jetty.websocket.api.Session

class WebSocketEndPoint(private val playerID: String)(using globalState: GlobalState, commands: Commands)
    extends Session.Listener.AbstractAutoDemanding with SLF4JLogging:

    import globalState.*

    private var session: Session = null
    private var player: PlayerCharacter = null

    override def onWebSocketOpen(session: Session) =
        this.session = session
        val startingRoom = rooms.head._2
        player = startingRoom.createPlayerCharacter(playerID, WebSocketConnection(session))
        log.info(s"Connection from ${player.name}@${session.getRemoteSocketAddress}")

    override def onWebSocketText(input: String) =
        commands.executeCommand(player, input)

    override def onWebSocketError(cause: Throwable) =
        cause.getClass.getSimpleName match
            case "WebSocketTimeoutException" =>
                log.info(s"Connection closed ${player.name}@${session.getRemoteSocketAddress}: ${cause.getMessage}")
            case _ =>
                log.error("ERROR", cause)

    override def onWebSocketClose(statusCode: Int, reason: String) =
        log.info(s"Connection closed ${player.name}@${session.getRemoteSocketAddress}, statusCode: $statusCode, reason: $reason")
