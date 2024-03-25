package core.connection

trait Connection:

    def readLine(): String
    def write(output: Output): Unit

    def close(): Unit
    def isClosed: Boolean
    
    
case class Output(message: String, map: String)