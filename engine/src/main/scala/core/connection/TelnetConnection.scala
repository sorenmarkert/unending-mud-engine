package core.connection

import java.io.*
import java.net.Socket

case class TelnetConnection(private val socket: Socket) extends Connection:

    private val reader = new BufferedReader(new InputStreamReader(socket.getInputStream))
    private val writer = new PrintWriter(socket.getOutputStream, true)

    override val readLine: () => String   = reader.readLine
    override val write   : String => Unit = writer.println

    def close() =
        reader.close()
        writer.close()
        socket.close()

    def isClosed() = socket.isClosed
