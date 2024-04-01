package core.connection

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import core.Colour
import core.Colour.*
import org.eclipse.jetty.websocket.api.{Callback, Session}

class WebSocketConnection(private val session: Session) extends Connection:

    val jsonMapper = JsonMapper.builder().addModule(DefaultScalaModule).build()

    override def readLine() = ???

    override def sendEnqueuedMessages(prompt: Seq[String], miniMap: Seq[String]) =
        val output = Output(messageQueue.toSeq, prompt, miniMap)
        messageQueue.clear()
        session.sendText(jsonMapper.writeValueAsString(output), Callback.NOOP)

    override def close() =
        session.close()

    override def isClosed = !session.isOpen

    override def substituteColourCodes(colour: Colour): String =
        colour match
            case Black => "<span style=\"color:black\">"
            case Red => "<span style=\"color:red\">"
            case Green => "<span style=\"color:green\">"
            case Yellow => "<span style=\"color:yellow\">"
            case Blue => "<span style=\"color:blue\">"
            case Magenta => "<span style=\"color:magenta\">"
            case Cyan => "<span style=\"color:cyan\">"
            case White => "<span style=\"color:white\">"
            case BrightBlack => "<span style=\"color:black;font-weight:bold;\">"
            case BrightRed => "<span style=\"color:red;font-weight:bold;\">"
            case BrightGreen => "<span style=\"color:green;font-weight:bold;\">"
            case BrightYellow => "<span style=\"color:yellow;font-weight:bold;\">"
            case BrightBlue => "<span style=\"color:blue;font-weight:bold;\">"
            case BrightMagenta => "<span style=\"color:magenta;font-weight:bold;\">"
            case BrightCyan => "<span style=\"color:cyan;font-weight:bold;\">"
            case BrightWhite => "<span style=\"color:white;font-weight:bold;\">"
            case Reset => "</span>"


case class Output(message: Seq[String], prompt: Seq[String], miniMap: Seq[String])