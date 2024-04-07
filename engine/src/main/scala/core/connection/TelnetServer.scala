package core.connection

import akka.event.slf4j.SLF4JLogging
import com.typesafe.config.Config
import core.commands.Commands
import core.gameunit.PlayerCharacter
import core.state.GlobalState
import core.state.RunState.Running
import core.storage.Storage

import java.net.{ServerSocket, Socket}
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Using}

object TelnetServer extends SLF4JLogging:

    def apply(config: Config)(using globalState: GlobalState, commands: Commands, storage: Storage): Future[Unit] =

        import globalState.*

        val port = config.getInt("telnet.port")
        log.info("Starting telnet client on port " + port)

        @tailrec
        def chooseName(state: LoginState, connection: TelnetConnection): String =
            state.nextState(connection) match
                case Done(name) => name.toLowerCase.capitalize
                case nextState: _ => chooseName(nextState, connection)

        def initConnection(socket: Socket): Unit =
            val connection = TelnetConnection(socket)
            val clientIpAddress = socket.getInetAddress.getHostAddress
            log.info(s"Connection from $clientIpAddress")

            val name = chooseName(NamePrompt, connection)
            log.info(s"Login from $name@$clientIpAddress")

            storage.loadPlayer(name, connection, playerCharacter => Future {
                serve(playerCharacter)
                socket.close()
                log.info(s"Connection closed $name@$clientIpAddress")
                storage.savePlayer(playerCharacter)
                playerCharacter.destroy
            })

        @tailrec
        def serve(playerCharacter: PlayerCharacter): Unit =
            Try {
                val input = playerCharacter.connection.readLine()
                commands.executeCommandAtNextTick(playerCharacter, input)
            }
            if !playerCharacter.connection.isClosed
                && runState == Running then serve(playerCharacter)

        Future {
            Using(ServerSocket(port)) { serverSocket =>
                while runState == Running do
                    val socket = serverSocket.accept
                    Future(initConnection(socket))
            }
        }


    private sealed trait LoginState:
        def nextState(connection: TelnetConnection): LoginState

    private case object NamePrompt extends LoginState:
        override def nextState(connection: TelnetConnection): LoginState =
            connection.write("What's your name?")
            val name = connection.readLine()
            NameVerify(name)

    private case class NameVerify(name: String) extends LoginState:
        override def nextState(connection: TelnetConnection): LoginState =
            connection.write(s"Is $name correct?")
            val answer = connection.readLine()
            if "yes".startsWith(answer) then Done(name) else NamePrompt

    private case class PasswordPrompt(name: String) extends LoginState:
        override def nextState(connection: TelnetConnection): LoginState = ???

    private case class Done(name: String) extends LoginState:
        override def nextState(connection: TelnetConnection): LoginState = throw IllegalStateException()
