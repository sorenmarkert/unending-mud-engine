package core.connection

import akka.actor.typed.ActorSystem
import akka.event.slf4j.SLF4JLogging
import com.typesafe.config.Config
import core.commands.Commands
import core.gameunit.PlayerCharacter
import core.state.{GlobalState, StateActorMessage}
import core.state.RunState.Running
import core.storage.Storage

import java.net.{ServerSocket, Socket}
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Using}

class TelnetServer(using actorSystem: ActorSystem[StateActorMessage], config: Config, globalState: GlobalState, commands: Commands, storage: Storage) extends SLF4JLogging:

    import globalState.runState

    private val port = config.getInt("telnet.port")
    log.info("Starting telnet client on port " + port)

    private def doLoginMenuAndGetPlayerCharacter(state: LoginState, connection: TelnetConnection): Future[PlayerCharacter] =
        state.nextState(connection).flatMap {
            case Done(playerCharacter) => Future.successful(playerCharacter)
            case nextState: _ => doLoginMenuAndGetPlayerCharacter(nextState, connection)
        }

    private def initConnection(socket: Socket): Unit =
        val connection = TelnetConnection(socket)
        val clientIpAddress = socket.getInetAddress.getHostAddress
        log.info(s"Connection from $clientIpAddress")

        doLoginMenuAndGetPlayerCharacter(CreateOrLogin, connection)
            .foreach { playerCharacter =>
                log.info(s"Login from ${playerCharacter.name}@$clientIpAddress")
                serve(playerCharacter)
                socket.close()
                log.info(s"Connection closed ${playerCharacter.name}@$clientIpAddress")
                storage.savePlayer(playerCharacter)
                playerCharacter.destroy
            }

    @tailrec
    private def serve(playerCharacter: PlayerCharacter): Unit =
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
        def nextState(connection: TelnetConnection): Future[LoginState]

    private case object CreateOrLogin extends LoginState:
        override def nextState(connection: TelnetConnection): Future[LoginState] =
            connection.write("Welcome! Create new char?")
            val name = connection.readLine().toLowerCase
            Future.successful(if "yes".startsWith(name) then NewCharName else NamePrompt)

    private case object NewCharName extends LoginState:
        override def nextState(connection: TelnetConnection): Future[LoginState] =
            connection.write("What will be your name?")
            val name = connection.readLine()
            Future.successful(IsNameAvailable(name))

    private case class IsNameAvailable(name: String) extends LoginState:
        override def nextState(connection: TelnetConnection): Future[LoginState] =
            storage.isNameAvailable(name)
                .map(if _ then
                    CreateChar(name)
                else
                    connection.write("That name is not available.")
                    NewCharName)

    private case class CreateChar(name: String) extends LoginState:
        override def nextState(connection: TelnetConnection): Future[LoginState] =
            val startingRoom = globalState.rooms("roomCenter")
            val playerCharacter = startingRoom.createPlayerCharacter(name, connection)
            log.info(s"Created player $name")
            Future.successful(Done(playerCharacter))

    private case object NamePrompt extends LoginState:
        override def nextState(connection: TelnetConnection): Future[LoginState] =
            connection.write("What's your name?")
            val name = connection.readLine()
            Future.successful(NameVerify(name))

    private case class NameVerify(name: String) extends LoginState:
        override def nextState(connection: TelnetConnection): Future[LoginState] =
            connection.write(s"Is $name correct?")
            val answer = connection.readLine()
            Future.successful(if "yes".startsWith(answer) then LoadChar(name) else NamePrompt)

    private case class LoadChar(name: String) extends LoginState:
        override def nextState(connection: TelnetConnection): Future[LoginState] =
            storage.loadPlayer(name, connection)
                .map {
                    case Some(playerCharacter) =>
                        log.info(s"Loaded player $name")
                        Done(playerCharacter)
                    case None =>
                        connection.write("No character goes by that name.")
                        NamePrompt
                }

    private case class Done(playerCharacter: PlayerCharacter) extends LoginState:
        override def nextState(connection: TelnetConnection): Future[LoginState] = throw IllegalStateException()
