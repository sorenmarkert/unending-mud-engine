package core.connection

trait Connection:

    def readLine(): String
    def write(text: String): Unit

    def close(): Unit
    def isClosed: Boolean