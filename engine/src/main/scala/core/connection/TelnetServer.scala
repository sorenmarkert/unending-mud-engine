package core.connection

import akka.event.slf4j.Logger
import com.typesafe.config.Config
import core.gameunit.GameUnit.createPlayerCharacterIn
import core.GlobalState.runState
import core.RunState.Running
import core.ZoneData.roomCenter
import core.commands.Commands.executeCommand
import core.gameunit.PlayerCharacter

import java.net.{ServerSocket, Socket}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object TelnetServer:

    private val logger = Logger("Telnet")

    def apply(config: Config) =

        val port = config.getInt("telnet.port")

        logger.warn("Starting telnet client on port " + port)

        def initConnection(socket: Socket) =
            val player = createPlayerCharacterIn(roomCenter, TelnetConnection(socket))
            logger.warn(s"Connection from ${player.name}@${socket.getInetAddress.getHostAddress}")
            serve(player)
            socket.close()
            logger.warn(s"Connection closed ${player.name}@${socket.getInetAddress.getHostAddress}")

        def serve(player: PlayerCharacter): Unit =
            val input       = player.connection.readLine()
            val commandWord = executeCommand(player, input)
            if commandWord != "quit" then serve(player)

        Future {
            val serverSocket = new ServerSocket(port)

            while runState == Running do
                val socket = serverSocket.accept
                Future(initConnection(socket))

            serverSocket.close()
        }

    end apply

end TelnetServer
