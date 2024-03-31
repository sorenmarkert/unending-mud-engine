package core

import core.connection.{Output, WebSocketConnection}
import core.gameunit.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.*
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class MessageSenderTest extends AnyWordSpec with MockitoSugar with GivenWhenThen with BeforeAndAfterEach with BeforeAndAfterAll:

    val messageSender = new MessageSender

    val roomMock = mock[Room]
    when(roomMock.mobiles).thenReturn(List())

    val playerMock = mock[PlayerCharacter]

    "sendMessage" should {

        "Wrap lines longer than textWidth" in {

            Given("A player with a connection")
            val connectionMock = mock[WebSocketConnection]
            when(connectionMock.substituteColourCodes(any())).thenCallRealMethod()
            when(playerMock.connection).thenReturn(connectionMock)

            When("Sending a message longer than 2x textWidth (50)")
            val message1 = "This message string is exactly 49 characters long"
            val message2 = "and it continues in this string which is also 49"
            val message3 = "and then it ends here."
            messageSender.sendMessage(playerMock, s"$message1 $message2 $message3", addPrompt = false)

            Then("It's sent as three lines")
            verify(connectionMock).send(Output(Seq(message1, message2, message3), Seq(), Seq()))
        }

        "Retain line breaks of input" in {

            Given("A player with a connection")
            val connectionMock = mock[WebSocketConnection]
            when(connectionMock.substituteColourCodes(any())).thenCallRealMethod()
            when(playerMock.connection).thenReturn(connectionMock)

            When("Sending a message with line breaks")
            val message = "This message\nhas two\nline breaks."
            messageSender.sendMessage(playerMock, message, addPrompt = false)

            Then("The line breaks are retained")
            verify(connectionMock).send(Output(message.linesIterator.toList, Seq(), Seq()))
        }

        "Replace colour codes" in {

            Given("A player with a connection")
            val connectionMock = mock[WebSocketConnection]
            when(connectionMock.substituteColourCodes(any())).thenCallRealMethod()
            when(playerMock.connection).thenReturn(connectionMock)

            When("Sending a message with colour codes")
            val message = "Here $Greenare so$BrightMagentame colo$Reseturs"
            messageSender.sendMessage(playerMock, message, addPrompt = false)

            Then("The colour codes have been replace with connection specific style formatting")
            val expectedMessage = "Here <span style=\"color:green\">are so<span style=\"color:magenta;font-weight:bold;\">me colo</span>urs"
            verify(connectionMock).send(Output(Seq(expectedMessage), Seq(), Seq()))
        }

        "Not count colour codes when wrapping words" in {

            Given("A player with a connection")
            val connectionMock = mock[WebSocketConnection]
            when(connectionMock.substituteColourCodes(any())).thenCallRealMethod()
            when(playerMock.connection).thenReturn(connectionMock)

            When("Sending a long message with colour codes")
            val message1 = "This message $BrightMagentastring has colour codes and it continues"
            val message2 = "in the next $Reset string."
            messageSender.sendMessage(playerMock, message1 + " " + message2, addPrompt = false)

            Then("Word wrap counts the message without formatting")
            val expectedMessage1 = "This message <span style=\"color:magenta;font-weight:bold;\">string has colour codes and it"
            val expectedMessage2 = "continues in the next </span> string."
            verify(connectionMock).send(Output(Seq(expectedMessage1, expectedMessage2), Seq(), Seq()))
        }

        "Add the prompt" in {

            Given("A player with a connection")
            val connectionMock = mock[WebSocketConnection]
            when(connectionMock.substituteColourCodes(any())).thenCallRealMethod()
            when(playerMock.connection).thenReturn(connectionMock)

            When("Sending a message")
            val message = "This is a message"
            messageSender.sendMessage(playerMock, message)

            Then("It's sent with the prompt")
            verify(connectionMock).send(Output(Seq(message), Seq("(12/20) fake-prompt (12/20)"), Seq()))
        }

        "Add the mini map with frame and colours" in {

            Given("Some rooms")
            val roomCenter = Room("roomCenter")
            Room("roomNorth")
                .northTo(roomCenter)

            And("A player with a connection in a room")
            val connectionMock = mock[WebSocketConnection]
            when(connectionMock.substituteColourCodes(any())).thenCallRealMethod()
            when(playerMock.connection).thenReturn(connectionMock)
            when(playerMock.outside).thenReturn(roomCenter)

            When("Sending a long message with colour codes")
            val message = "This is a message"
            messageSender.sendMessage(playerMock, message, addMiniMap = true)

            Then("It's sent as three lines")
            verify(connectionMock).send(
                Output(
                    Seq(message),
                    Seq("(12/20) fake-prompt (12/20)"),
                    Seq(
                        "/-------------------\\",
                        "|                   |",
                        "|                   |",
                        "|                   |",
                        "|                   |",
                        "|                   |",
                        "|                   |",
                        "|         <span style=\"color:red;font-weight:bold;\">X</span>         |",
                        "|         |         |",
                        "|         <span style=\"color:yellow\">#</span>         |",
                        "|                   |",
                        "|                   |",
                        "|                   |",
                        "|                   |",
                        "\\-------------------/"
                    )))
        }

        "Send to controller of an NPC" is pending
    }

    // TODO: test this
    "act" is pending
