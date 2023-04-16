package core

import core.commands.Commands
import core.connection.Connection
import core.gameunit.*
import core.gameunit.GameUnit.createPlayerCharacterIn
import org.mockito.Mockito.*
import org.scalatest.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.mutable.ListBuffer

class MessageSenderTest extends AnyWordSpec with MockitoSugar with GivenWhenThen with BeforeAndAfterEach with BeforeAndAfterAll:
    
    val messageSender = new MessageSender
    import messageSender.*
    
    val roomMock = mock[Room]
    when(roomMock.contents).thenReturn(List())
    
    val playerMock = mock[PlayerCharacter]

    // TODO: test this
    "sendMessage" should {

        "Wrap lines longer than textWidth" in {

            Given("A player with a connection")
            val connectionMock = mock[Connection]
            when(playerMock.connection).thenReturn(connectionMock)

            When("Sending a message longer than 50")
            val message = (1 to 32).mkString
//            sendMessage(playerMock, message)

            Then("It's sent as two lines")
//            verify(connectionMock).write("asdasd")
        }
        
        "Retain line breaks of input" is pending
        
        "Send to controller of an NPC" is pending
        
        "Add minimap" is pending
    }

    "act" is pending
