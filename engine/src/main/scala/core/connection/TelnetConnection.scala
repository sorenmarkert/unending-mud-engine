package core.connection

import java.io.*
import java.net.Socket

case class TelnetConnection(private val socket: Socket) extends Connection:

    private val reader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    private val writer = new PrintWriter(socket.getOutputStream, true)

    override def readLine() = reader.readLine()

    override def write(text: String) = writer.println(text)

    def close() =
        socket.close()
        reader.close()
        writer.close()

    def isClosed = socket.isClosed
