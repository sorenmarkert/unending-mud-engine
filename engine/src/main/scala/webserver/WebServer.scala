package webserver

import akka.event.slf4j.{Logger, SLF4JLogging}
import com.typesafe.config.Config

object WebServer extends SLF4JLogging:

    def apply()(using config: Config): Unit =

        val interface = config.getString("http.interface")
        val port = config.getInt("http.port")

//        log.info("Starting web server on port " + port)
