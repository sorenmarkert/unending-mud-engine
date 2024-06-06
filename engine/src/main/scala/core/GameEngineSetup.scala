package core

import akka.actor.typed.ActorSystem
import akka.event.slf4j.SLF4JLogging
import com.typesafe.config.{Config, ConfigFactory}
import core.commands.*
import core.connection.{TelnetServer, WebSocketServer}
import core.state.RunState.Running
import core.state.{GlobalState, StateActor}
import core.storage.*
import webserver.WebServer

object GameEngineSetup extends SLF4JLogging:

    given config: Config = ConfigFactory.load()

    given ActorSystem[StateActor.Message] = ActorSystem(StateActor(), "unending")

    given Commands = Commands()

    given Storage =
        if config.getBoolean("storage.useMongo") then
            MongoDbStorage()
        else
            RavenDBStorage()


    @main def startEngine(): Unit =

        log.info("Starting the Unending MUD Engine.")

        summon[GlobalState].runState = Running
        ZoneData
        TelnetServer()
        WebSocketServer()
        WebServer()
