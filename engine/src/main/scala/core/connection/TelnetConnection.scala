package core.connection

import core.Colour
import core.Colour.*

import java.io.*
import java.net.Socket

class TelnetConnection(private val socket: Socket) extends Connection:

    private val usualMiniMapWidth = 21

    private val reader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    private val writer = new PrintWriter(socket.getOutputStream, true)

    override def readLine(): String = reader.readLine()

    override def sendEnqueuedMessages(prompt: Seq[String], miniMap: Seq[String]): Unit =
        writer.println(combineMessageWithPromptAndMiniMap(messageQueue.toSeq, prompt, miniMap))
        messageQueue.clear()


    override def close(): Unit =
        socket.close()
        reader.close()
        writer.close()

    override def isClosed: Boolean = socket.isClosed

    override def substituteColourCodes(colour: Colour): String =
        // ANSI colour codes https://gist.github.com/fnky/458719343aabd01cfb17a3a4f7296797
        colour match
            case Black => "\u001b[30m"
            case Red => "\u001b[31m"
            case Green => "\u001b[32m"
            case Yellow => "\u001b[33m"
            case Blue => "\u001b[34m"
            case Magenta => "\u001b[35m"
            case Cyan => "\u001b[36m"
            case White => "\u001b[37m"
            case BrightBlack => "\u001b[90;1m"
            case BrightRed => "\u001b[91;1m"
            case BrightGreen => "\u001b[92;1m"
            case BrightYellow => "\u001b[93;1m"
            case BrightBlue => "\u001b[94;1m"
            case BrightMagenta => "\u001b[95;1m"
            case BrightCyan => "\u001b[96;1m"
            case BrightWhite => "\u001b[97;1m"
            case Reset => "\u001b[0m"

    private def combineMessageWithPromptAndMiniMap(message: Seq[String], prompt: Seq[String], miniMap: Seq[String]) =

        val miniMapWidth = miniMap.map(_.length).maxOption.getOrElse(usualMiniMapWidth)
        val miniMapWideSpace = "".padTo(miniMapWidth, ' ')
        val miniMapOfFinalHeight = miniMap.padTo(message.size + prompt.size, miniMapWideSpace)

        val messageWithPromptOfFinalHeight = message.padTo(miniMap.size - prompt.size, "").appendedAll(prompt)

        miniMapOfFinalHeight
            .zip(messageWithPromptOfFinalHeight)
            .map((a, b) => a + "  " + b)
            .mkString("\n")
