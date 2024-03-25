package core.connection

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.eclipse.jetty.websocket.api.{Callback, Session}

class WebSocketConnection(private val session: Session) extends Connection:

    val jsonMapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

    override def readLine() = ???

    override def write(output: Output) = session.sendText(jsonMapper.writeValueAsString(output), Callback.NOOP)

    override def close() =
        session.close()

    override def isClosed = !session.isOpen
