package core.connection

import core.GlobalState.{Running, runState}
import core.GameUnit.createPlayerCharacterIn
import core.PlayerCharacter
import core.ZoneData.roomCenter
import core.commands.Commands.executeCommand
import play.api.Logger

import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}

object TelnetServer {

    private val logger = Logger(this.getClass)

    def apply(port: Int)(implicit exec: ExecutionContext) = {

        logger.warn("Starting telnet client on port " + port)

        def initConnection(socket: Socket) = {

            val player = createPlayerCharacterIn(roomCenter, TelnetConnection(socket))

            serve(player)

            socket.close()
            logger.warn("Connection closed from " + socket.getInetAddress.getHostAddress)
        }

        def serve(player: PlayerCharacter): Unit = {

            val input = player.connection.readLine()
            val commandWord = executeCommand(player, input)

            if (commandWord != "quit") serve(player)
        }

        Future {
            val serverSocket = new ServerSocket(port)
            while (runState == Running) {
                val socket = serverSocket.accept
                logger.warn("Connection from " + socket.getInetAddress.getHostAddress)

                Future(initConnection(socket))
            }
            serverSocket.close()
        }
    }
}