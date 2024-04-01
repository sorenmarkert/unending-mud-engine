package core.connection

import core.Colour

import scala.collection.mutable

trait Connection:

    protected val messageQueue = mutable.Queue.empty[String]

    def enqueueMessage(message: Seq[String]) = messageQueue.enqueueAll(message)

    def readLine(): String

    def sendEnqueuedMessages(prompt: Seq[String], miniMap: Seq[String]): Unit

    def close(): Unit

    def isClosed: Boolean

    def substituteColourCodes(colour: Colour): String
