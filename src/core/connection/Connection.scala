package core.connection

import akka.actor.typed.ActorSystem
import core.StateActor

trait Connection {

    val actorSystem: ActorSystem[StateActor.StateActorMessage]

    val readLine: () => String
    val write: String => Unit

    def close(): Unit
}
