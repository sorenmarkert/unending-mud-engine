package webserver

import akka.actor.typed.ActorSystem
import akka.event.slf4j.Logger
import akka.http.scaladsl.*
import akka.http.scaladsl.model.ws.*
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}
import core.GlobalState
import core.StateActor.StateActorMessage

import java.nio.file.Paths
import scala.concurrent.ExecutionContext

object WebServer extends Directives :

    private val logger = Logger("WebServer")

    given system: ActorSystem[StateActorMessage] = GlobalState.actorSystem

    given ExecutionContext = system.executionContext

    def apply(config: Config) =

        val interface = config.getString("http.interface")
        val port      = config.getInt("http.port")

        logger.info("Starting web server on port " + port)

        Http()
            .newServerAt(interface, port)
            .bindFlow(index ~ assets ~ websocketRoute)

    private val index: Route =
        pathSingleSlash(
            getFromResource("index.html")
            )

    private val assets: Route =
        path("assets" / Remaining) { file =>
            getFromResource("assets/" + file)
        }

    private def greeter: Flow[Message, Message, Any] =
        Flow[Message].mapConcat {
            case tm: TextMessage   =>
                TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
            case bm: BinaryMessage =>
                // ignore binary messages but drain content to avoid the stream being clogged
                bm.dataStream.runWith(Sink.ignore)
                Nil
        }

    private val websocketRoute: Route =
        path("greeter") {
            handleWebSocketMessages(greeter)
        }