package core

import akka.event.slf4j.Logger
import com.typesafe.config.ConfigFactory
import core.GlobalState.runState
import core.RunState.Running
import core.connection.TelnetServer

import scala.concurrent.ExecutionContext

object GameEngineSetup:

    private val logger = Logger("Setup")

    @main def startEngine =

        logger.warn("Starting the Unending MUD Engine.")

        val config = ConfigFactory.load()
        val telnetPort = config.getInt("engine.telnet.port")

        runState = Running
        TelnetServer(telnetPort)
