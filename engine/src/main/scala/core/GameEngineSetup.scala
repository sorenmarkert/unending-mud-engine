package core

import akka.event.slf4j.Logger
import com.typesafe.config.ConfigFactory
import core.RunState.Running
import core.connection.TelnetServer
import webserver.WebServer

import scala.concurrent.ExecutionContext

object GameEngineSetup:

    private val logger = Logger("Setup")

    @main def startEngine =

        logger.info("Starting the Unending MUD Engine.")

        val config = ConfigFactory.load()

        summon[GlobalState].runState = Running
        ZoneData //  TODO: find a better solution to make this run
        TelnetServer(config)
        WebServer(config)
