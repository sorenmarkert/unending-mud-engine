package core.connection

import akka.event.slf4j.{Logger, SLF4JLogging}
import com.typesafe.config.Config
import core.commands.Commands
import core.gameunit.RunState.Running
import core.gameunit.{GlobalState, PlayerCharacter}

import java.net.{ServerSocket, Socket}
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

object TelnetServer extends SLF4JLogging:

    def apply(config: Config)(using globalState: GlobalState, commands: Commands): Future[Unit] =

        import globalState.*

        val port = config.getInt("telnet.port")

        log.info("Starting telnet client on port " + port)

        def initConnection(socket: Socket) =
            // TODO: save and load room with player
            val startingRoom = rooms.head._2
            val player = startingRoom.createPlayerCharacter("player1", TelnetConnection(socket))
            log.info(s"Connection from ${player.name}@${socket.getInetAddress.getHostAddress}")
            serve(player)
            socket.close()
            log.info(s"Connection closed ${player.name}@${socket.getInetAddress.getHostAddress}")

        @tailrec
        def serve(player: PlayerCharacter): Unit =
            val input = player.connection.readLine()
            commands.executeCommand(player, input)
            if !player.connection.isClosed
                && runState == Running then serve(player)

        Future {
            Using(ServerSocket(port)) { serverSocket =>
                while runState == Running do
                    val socket = serverSocket.accept
                    Future(initConnection(socket))
            }
        }
