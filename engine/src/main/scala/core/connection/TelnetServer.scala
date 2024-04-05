package core.connection

import akka.event.slf4j.SLF4JLogging
import com.typesafe.config.Config
import core.commands.Commands
import core.gameunit.PlayerCharacter
import core.state.GlobalState
import core.state.RunState.Running

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

        def initConnection(socket: Socket): Unit =
            val startingRoom = rooms.head._2
            val player = startingRoom.createPlayerCharacter("player1", TelnetConnection(socket))
            log.info(s"Connection from ${player.name}@${socket.getInetAddress.getHostAddress}")
            serve(player)
            socket.close()
            log.info(s"Connection closed ${player.name}@${socket.getInetAddress.getHostAddress}")

        @tailrec
        def serve(player: PlayerCharacter): Unit =
            val input = player.connection.readLine()
            commands.executeCommandAtNextTick(player, input)
            if !player.connection.isClosed
                && runState == Running then serve(player)

        Future {
            Using(ServerSocket(port)) { serverSocket =>
                while runState == Running do
                    val socket = serverSocket.accept
                    Future(initConnection(socket))
            }
        }
