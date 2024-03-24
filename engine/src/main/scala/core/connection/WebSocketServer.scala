package core.connection

import akka.event.slf4j.SLF4JLogging
import com.typesafe.config.Config
import core.commands.Commands
import core.gameunit.GlobalState
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler

import scala.concurrent.Future

object WebSocketServer extends SLF4JLogging:

    def apply(config: Config)(using globalState: GlobalState, commands: Commands) =

        import globalState.*

        val port = config.getInt("websocket.port")

        log.info("Starting websocket client on port " + port)

        val server = new Server(8080)
        val contextHandler = new ContextHandler("/ws")
        server.setHandler(contextHandler)

        val webSocketHandler = WebSocketUpgradeHandler.from(server, contextHandler, container => {

                container.setMaxTextMessageSize(128 * 1024)
                container.addMapping("/main", (upgradeRequest, upgradeResponse, callback)=> {
                    new WebSocketEndPoint("player1")
                })
        })

        contextHandler.setHandler(webSocketHandler)
        server.start()