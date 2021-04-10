package core.connection

import akka.actor.typed.ActorSystem
import core.GameState.{Running, runState}
import core.GameUnit.createPlayerCharacterIn
import core.ZoneData.roomCenter
import core.commands.Commands.executeCommand
import core.{PlayerCharacter, StateActor}
import play.api.Logger

import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}

object TelnetServer {

    private val logger = Logger(this.getClass)

    def apply(port: Int, actorSystem: ActorSystem[StateActor.StateActorMessage])(implicit exec: ExecutionContext) = {

        logger.warn("Starting telnet client on port " + port)

        def initConnection(socket: Socket) = {

            val player = createPlayerCharacterIn(roomCenter, TelnetConnection(socket, actorSystem))

            serve(player)

            socket.close()
            player.removeUnit
            logger.warn("Connection closed from " + socket.getInetAddress.getHostAddress)
        }

        def serve(player: PlayerCharacter): Unit = {

            val input = player.connection.readLine()
            val commandWord = executeCommand(player, input, actorSystem)

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