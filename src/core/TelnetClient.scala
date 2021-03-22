package core

import core.commands.Commands.executeCommand
import core.ZoneData.north
import core.GameState.runState
import core.GameUnit.createPlayerCharacter
import play.api.{Configuration, Logger}

import java.io.{BufferedReader, InputStream, InputStreamReader, PrintWriter}
import java.net.{ServerSocket, Socket}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TelnetClient @Inject()(conf: Configuration)(implicit exec: ExecutionContext) {

    private val logger = Logger(this.getClass)

    Future(start())

    def start() = {
        val port = conf.get[Int]("engine.telnet.port")
        logger.warn("Starting telnet client on port " + port)

        val acceptor = new ServerSocket(port)
        runState = Running
        while (runState == Running) {
            val socket = acceptor.accept
            logger.warn("Connection from " + socket.getInetAddress.getHostAddress)

            Future(initConnection(socket))
        }
    }

    private def initConnection(socket: Socket) = {
        val player = createPlayerCharacter(north, Connected, new PrintWriter(socket.getOutputStream, true))
        // TODO: load player data
        player.id = "player1"
        player.name = "klaus"
        player.title = "Klaus Hansen"
        player.connectionState = Connected
        serve(socket.getInputStream, player)
        socket.close()
        logger.warn("Connection closed from " + socket.getInetAddress.getHostAddress)
    }

    private def serve(in: InputStream, player: PlayerCharacter): Unit = {

        val input = new BufferedReader(new InputStreamReader(in)).readLine()
        executeCommand(player, input)

        if (player.connectionState == Disconnecting) {
            // TODO: save player and remove the player object
            player.removeUnit
        } else {
            serve(in, player)
        }
    }
}

sealed trait ConnectionState

case object Connecting extends ConnectionState

case object Connected extends ConnectionState

case object Disconnecting extends ConnectionState
