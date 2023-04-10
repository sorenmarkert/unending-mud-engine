package core.connection

import akka.event.slf4j.Logger
import com.typesafe.config.Config
import core.GlobalState
import core.Messaging.sendMessage
import core.RunState.Running
import core.commands.Commands
import core.gameunit.GameUnit.createPlayerCharacterIn
import core.gameunit.PlayerCharacter

import java.net.{ServerSocket, Socket}
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object TelnetServer:

    private val logger = Logger("Telnet")

    def apply(config: Config)(using globalState: GlobalState, commands: Commands): Future[Unit] =
        import globalState.*

        val port = config.getInt("telnet.port")

        logger.info("Starting telnet client on port " + port)

        def initConnection(socket: Socket) =
            // TODO: save and load room with player
            val startingRoom = rooms.head._2
            val player       = createPlayerCharacterIn(startingRoom, TelnetConnection(socket))
            logger.info(s"Connection from ${player.name}@${socket.getInetAddress.getHostAddress}")
            serve(player)
            socket.close()
            logger.info(s"Connection closed ${player.name}@${socket.getInetAddress.getHostAddress}")

        @tailrec
        def serve(player: PlayerCharacter): Unit =
            val input = player.connection.readLine()
            commands.executeCommand(player, input)
            if !player.connection.isClosed()
                && runState == Running then serve(player)

        Future {
            val serverSocket = new ServerSocket(port)

            while runState == Running do
                val socket = serverSocket.accept
                Future(initConnection(socket))

            serverSocket.close()
        }
