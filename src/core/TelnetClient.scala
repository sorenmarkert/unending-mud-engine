package core

import akka.actor.typed.ActorSystem
import core.GameState.runState
import core.GameUnit.createPlayerCharacterIn
import core.ZoneData.roomCenter
import core.commands.Commands.{act, executeCommand}
import core.commands.{Always, ToAllExceptActor}
import play.api.Logger

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import scala.concurrent.{ExecutionContext, Future}

object TelnetClient {

    private val logger = Logger(this.getClass)

    def apply(port: Int, actorSystem: ActorSystem[StateActor.StateActorMessage])(implicit exec: ExecutionContext) = {

        logger.warn("Starting telnet client on port " + port)

        def serve(reader: BufferedReader, player: PlayerCharacter): Unit = {

            val input = reader.readLine()
            executeCommand(player, input, actorSystem)

            // TODO: instant logout
            if (player.connectionState == Disconnecting) {
                // TODO: save player data
                player.removeUnit
            } else {
                serve(reader, player)
            }
        }

        def initConnection(socket: Socket) = {

            val player = createPlayerCharacterIn(roomCenter, Connected, new PrintWriter(socket.getOutputStream, true))
            // TODO: load player data
            player.id = "player1"
            player.name = "Klaus"
            player.title = "the Rude"
            player.connectionState = Connected
            executeCommand(player, "look", actorSystem)
            act("$1N has entered the game.", Always, Some(player), None, None, ToAllExceptActor, None)

            serve(new BufferedReader(new InputStreamReader(socket.getInputStream)), player)

            socket.close()
            player.removeUnit
            logger.warn("Connection closed from " + socket.getInetAddress.getHostAddress)
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

sealed trait ConnectionState

case object Connecting extends ConnectionState

case object Connected extends ConnectionState

case object Disconnecting extends ConnectionState
