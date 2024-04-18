package core.connection

import akka.event.slf4j.SLF4JLogging
import com.typesafe.config.Config
import core.commands.Commands
import core.state.GlobalState
import core.storage.Storage
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler

import java.time.Duration
import java.time.Duration.ofMinutes

object WebSocketServer extends SLF4JLogging:

    def apply(config: Config)(using commands: Commands, storage: Storage): Unit =

        val port = config.getInt("websocket.port")

        log.info("Starting websocket client on port " + port)

        val server = new Server(8080)

        val webSocketHandler = WebSocketUpgradeHandler.from(server, container => {
            container.setMaxTextMessageSize(128 * 1024)
            container.setMaxOutgoingFrames(100)
            container.setIdleTimeout(ofMinutes(5L))
            container.addMapping("/ws", (upgradeRequest, upgradeResponse, callback) => {
                new WebSocketEndPoint("player1")
            })
        })

        server.setHandler(webSocketHandler)
        server.start()
        