package core.connection

trait Connection:

    val readLine: () => String
    val write   : String => Unit

    def close(): Unit
