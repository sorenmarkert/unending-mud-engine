package core

import akka.event.slf4j.{Logger, SLF4JLogging}
import com.typesafe.config.{Config, ConfigFactory}
import core.commands.*
import core.connection.{TelnetServer, WebSocketServer}
import core.gameunit.GlobalState
import core.gameunit.RunState.Running
import core.storage.*
import webserver.WebServer

import scala.concurrent.ExecutionContext

object GameEngineSetup extends SLF4JLogging:

    given config: Config = ConfigFactory.load()

    given Storage =
        if config.getBoolean("storage.useMongo") then
            new MongoDbStorage()
        else
            new RavenDBStorage()

    given BasicCommands = new BasicCommands

    given CombatCommands = new CombatCommands

    given EquipmentCommands = new EquipmentCommands

    given Commands = new Commands
    

    @main def startEngine =

        log.info("Starting the Unending MUD Engine.")

        summon[GlobalState].runState = Running
        ZoneData //  TODO: find a better solution to make this run
        TelnetServer(config)
        WebSocketServer(config)
        WebServer(config)
