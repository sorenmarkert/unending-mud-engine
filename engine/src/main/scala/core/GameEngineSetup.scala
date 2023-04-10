package core

import akka.event.slf4j.Logger
import com.typesafe.config.{Config, ConfigFactory}
import core.RunState.Running
import core.commands.*
import core.connection.TelnetServer
import core.storage.{MongoDbStorage, Storage}
import webserver.WebServer

import scala.concurrent.ExecutionContext

object GameEngineSetup:

    private val logger = Logger("Setup")

    given config: Config = ConfigFactory.load()

    given BasicCommands = new BasicCommands

    given CombatCommands = new CombatCommands

    given EquipmentCommands = new EquipmentCommands

    given Commands = new Commands

    given Storage = new MongoDbStorage()


    @main def startEngine =

        logger.info("Starting the Unending MUD Engine.")

        summon[GlobalState].runState = Running
        ZoneData //  TODO: find a better solution to make this run
        TelnetServer(config)
        WebServer(config)
