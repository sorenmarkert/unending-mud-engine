package core

import core.ActRecipient.{ToActor, ToAllExceptActor, ToBystanders, ToEntireRoom, ToTarget}
import core.ActVisibility.Always
import core.connection.{Output, WebSocketConnection}
import core.gameunit.*
import core.gameunit.Gender.{GenderFemale, GenderMale}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.*
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class MessageSenderTest extends AnyWordSpec with MockitoSugar with GivenWhenThen with BeforeAndAfterEach:

    given globalState: GlobalState = new GlobalState()

    val thePrompt = "(12/20) fake-prompt (12/20)"

    val messageSender = new MessageSender

    val actingPlayerMock = mock[PlayerCharacter]
    val targetPlayerMock = mock[PlayerCharacter]
    val bystanderPlayerMock = mock[PlayerCharacter]
    val mediumItemMock = mock[Item]
    val roomMock = mock[Room]

    override def beforeEach() =
        globalState.clear()
        when(actingPlayerMock.outside).thenReturn(roomMock)
        when(roomMock.mobiles).thenReturn(Seq(actingPlayerMock, targetPlayerMock, bystanderPlayerMock))

    "sendMessage()" should {

        "Capitalize the message" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a message starting with a lower case letter")
            messageSender.sendMessage(actingPlayerMock, "message", addPrompt = false)

            Then("The message is sent capitalized")
            verify(connectionMock).send(Output(Seq("Message"), Seq(), Seq()))
        }

        "Wrap lines longer than textWidth" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a message longer than 2x textWidth (50)")
            val message1 = "This message string is exactly 49 characters long"
            val message2 = "and it continues in this string which is also 49"
            val message3 = "and then it ends here."
            messageSender.sendMessage(actingPlayerMock, s"$message1 $message2 $message3", addPrompt = false)

            Then("It's sent as three lines")
            verify(connectionMock).send(Output(Seq(message1, message2, message3), Seq(), Seq()))
        }

        "Retain line breaks of input" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a message with line breaks")
            val message = "This message\nhas two\nline breaks."
            messageSender.sendMessage(actingPlayerMock, message, addPrompt = false)

            Then("The line breaks are retained")
            verify(connectionMock).send(Output(message.linesIterator.toList, Seq(), Seq()))
        }

        "Replace colour codes" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a message with colour codes")
            val message = "Here $Greenare so$BrightMagentame colo$Reseturs"
            messageSender.sendMessage(actingPlayerMock, message, addPrompt = false)

            Then("The colour codes have been replace with connection specific style formatting")
            val expectedMessage = "Here <span style=\"color:green\">are so<span style=\"color:magenta;font-weight:bold;\">me colo</span>urs"
            verify(connectionMock).send(Output(Seq(expectedMessage), Seq(), Seq()))
        }

        "Not count colour codes when wrapping words" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a long message with colour codes")
            val message1 = "This message $BrightMagentastring has colour codes and it continues"
            val message2 = "in the next $Reset string."
            messageSender.sendMessage(actingPlayerMock, message1 + " " + message2, addPrompt = false)

            Then("Word wrap counts the message without formatting")
            val expectedMessage1 = "This message <span style=\"color:magenta;font-weight:bold;\">string has colour codes and it"
            val expectedMessage2 = "continues in the next </span> string."
            verify(connectionMock).send(Output(Seq(expectedMessage1, expectedMessage2), Seq(), Seq()))
        }

        "Add the prompt" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a message")
            val message = "This is a message"
            messageSender.sendMessage(actingPlayerMock, message)

            Then("It's sent with the prompt")
            verify(connectionMock).send(Output(Seq(message), Seq(thePrompt), Seq()))
        }

        "Add the mini map with frame and colours" in {

            Given("Some rooms")
            val roomCenter = Room("roomCenter")
            Room("roomNorth")
                .northTo(roomCenter)

            And("A player with a connection in a room")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)
            when(actingPlayerMock.outside).thenReturn(roomCenter)

            When("Sending a long message with colour codes")
            val message = "This is a message"
            messageSender.sendMessage(actingPlayerMock, message, addMiniMap = true)

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

    "act()" when {

        "Choosing recipient" should {

            "Send message only to Actor" in {

                Given("Characters in a room")
                val (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock) = setUpConnections

                When("Performing an act for Actor")
                val message = "Message"
                messageSender.act(message, Always, Some(actingPlayerMock), Some(mediumItemMock), Some(targetPlayerMock), ToActor, None)

                Then("It's sent only to Actor")
                verify(actingPlayerConnectionMock).send(Output(Seq(message), Seq(thePrompt), Seq()))
                verifyNoInteractions(targetPlayerConnectionMock)
                verifyNoInteractions(bystanderPlayerConnectionMock)
            }

            "Send message only to Target" in {

                Given("Characters in a room")
                val (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock) = setUpConnections

                When("Performing an act for Target")
                val message = "Message"
                messageSender.act("message", Always, Some(actingPlayerMock), Some(mediumItemMock), Some(targetPlayerMock), ToTarget, None)

                Then("It's sent only to Actor")
                verifyNoInteractions(actingPlayerConnectionMock)
                verify(targetPlayerConnectionMock).send(Output(Seq(message), Seq(thePrompt), Seq()))
                verifyNoInteractions(bystanderPlayerConnectionMock)
            }

            "Send message only to Bystanders" in {

                Given("Characters in a room")
                val (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock) = setUpConnections

                When("Performing an act for Bystanders")
                val message = "Message"
                messageSender.act("message", Always, Some(actingPlayerMock), Some(mediumItemMock), Some(targetPlayerMock), ToBystanders, None)

                Then("It's sent only to Actor")
                verifyNoInteractions(actingPlayerConnectionMock)
                verifyNoInteractions(targetPlayerConnectionMock)
                verify(bystanderPlayerConnectionMock).send(Output(Seq(message), Seq(thePrompt), Seq()))
            }

            "Send message only to All Except Actor" in {

                Given("Characters in a room")
                val (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock) = setUpConnections

                When("Performing an act for All Except Actor")
                val message = "Message"
                messageSender.act("message", Always, Some(actingPlayerMock), Some(mediumItemMock), Some(targetPlayerMock), ToAllExceptActor, None)

                Then("It's sent only to Actor")
                verifyNoInteractions(actingPlayerConnectionMock)
                verify(targetPlayerConnectionMock).send(Output(Seq(message), Seq(thePrompt), Seq()))
                verify(bystanderPlayerConnectionMock).send(Output(Seq(message), Seq(thePrompt), Seq()))
            }

            "Send message only to Entire Room" in {

                Given("Characters in a room")
                val (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock) = setUpConnections

                When("Performing an act for Entire Room")
                val message = "Message"
                messageSender.act("message", Always, Some(actingPlayerMock), Some(mediumItemMock), Some(targetPlayerMock), ToEntireRoom, None)

                Then("It's sent only to Actor")
                verify(actingPlayerConnectionMock).send(Output(Seq(message), Seq(thePrompt), Seq()))
                verify(targetPlayerConnectionMock).send(Output(Seq(message), Seq(thePrompt), Seq()))
                verify(bystanderPlayerConnectionMock).send(Output(Seq(message), Seq(thePrompt), Seq()))
            }
        }

        "Using unit formatters" should {

            "Replace units" in {

                Given("Characters in a room")
                val (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock) = setUpConnections
                when(actingPlayerMock.name).thenReturn("actorName")
                when(actingPlayerMock.gender).thenReturn(GenderFemale)
                when(targetPlayerMock.name).thenReturn("targetName")
                when(targetPlayerMock.gender).thenReturn(GenderMale)
                when(mediumItemMock.name).thenReturn("mediumItemName")

                When("Performing an act with unit formatters")
                val message = "$1N waves at $3N with $2a $2N, before $1e breaks $2e over $3m, $1s best $1t."
                messageSender.act(
                    message, Always,
                    Some(actingPlayerMock), Some(mediumItemMock), Some(targetPlayerMock),
                    ToBystanders, Some("enemy"))

                Then("It's sent only to Actor")
                val message1 = "ActorName waves at targetName with a"
                val message2 = "mediumItemName, before she breaks it over him, her"
                val message3 = "best enemy."
                verify(bystanderPlayerConnectionMock).send(Output(Seq(message1, message2, message3), Seq(thePrompt), Seq()))
            }
        }
    }

    private def setUpConnectionMockOnPlayer(playerMock: PlayerCharacter) = {
        val connectionMock = mock[WebSocketConnection]
        when(connectionMock.substituteColourCodes(any())).thenCallRealMethod()
        when(playerMock.connection).thenReturn(connectionMock)
        connectionMock
    }

    private def setUpConnections = {
        val actingPlayerConnectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)
        val targetPlayerConnectionMock = setUpConnectionMockOnPlayer(targetPlayerMock)
        val bystanderPlayerConnectionMock = setUpConnectionMockOnPlayer(bystanderPlayerMock)
        (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock)
    }
