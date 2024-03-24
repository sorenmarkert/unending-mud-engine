package core.connection

import org.eclipse.jetty.websocket.api.{Callback, Session}

import java.io.*
import java.net.Socket

case class WebSocketConnection(private val session: Session) extends Connection:

    override def readLine() = ???

    override def write(text: String) = session.sendText(text, Callback.NOOP)

    def close() =
        session.close()

    def isClosed = !session.isOpen
