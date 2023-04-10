package core

import akka.event.slf4j.{Logger, SLF4JLogging}
import com.typesafe.config.{Config, ConfigFactory}
import core.RunState.Running
import core.commands.*
import core.connection.TelnetServer
import core.storage.{MongoDbStorage, Storage}
import webserver.WebServer

import scala.concurrent.ExecutionContext

object GameEngineSetup extends SLF4JLogging:

    given config: Config = ConfigFactory.load()

    given BasicCommands = new BasicCommands

    given CombatCommands = new CombatCommands

    given EquipmentCommands = new EquipmentCommands

    given Commands = new Commands

    given Storage = new MongoDbStorage()


    @main def startEngine =

        log.info("Starting the Unending MUD Engine.")

        summon[GlobalState].runState = Running
        ZoneData //  TODO: find a better solution to make this run
        TelnetServer(config)
        WebServer(config)
