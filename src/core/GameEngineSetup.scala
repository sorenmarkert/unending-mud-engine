package core

import akka.actor.typed.ActorSystem
import core.GameState.runState
import play.api.{Configuration, Logger}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GameEngineSetup @Inject()(conf: Configuration)(implicit exec: ExecutionContext) {

    private val logger = Logger(this.getClass)

    startIt()

    private def startIt() = {

        logger.warn("Starting game engine.")

        val actorSystem = ActorSystem(StateActor(), "unending")
        val telnetPort = conf.get[Int]("engine.telnet.port")

        runState = Running
        TelnetClient(telnetPort, actorSystem)
    }
}
