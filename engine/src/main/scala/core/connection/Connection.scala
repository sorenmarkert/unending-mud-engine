package core.connection

import core.Colour

trait Connection:

    def readLine(): String
    def send(output: Output): Unit

    def close(): Unit
    def isClosed: Boolean
    
    def substituteColourCodes(colour: Colour): String
    
    
case class Output(message: Seq[String], prompt: Seq[String], miniMap: Seq[String])