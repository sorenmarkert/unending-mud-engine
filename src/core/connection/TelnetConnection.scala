package core.connection

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.net.Socket

case class TelnetConnection(private val socket: Socket) extends Connection {

    val reader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    val writer = new PrintWriter(socket.getOutputStream, true)

    override val readLine: () => String = reader.readLine
    override val write: String => Unit = writer.println

    def close(): Unit = {
        reader.close()
        writer.close()
        socket.close()
    }
}
