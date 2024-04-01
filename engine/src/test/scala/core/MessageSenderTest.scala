package core

import core.ActRecipient.*
import core.ActVisibility.Always
import core.connection.WebSocketConnection
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
            verify(connectionMock).enqueueMessage(Seq("Message"))
        }

        "Retain line breaks of input" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a message with line breaks")
            val message = "This message\nhas two\nline breaks."
            messageSender.sendMessage(actingPlayerMock, message, addPrompt = false)

            Then("The line breaks are retained")
            verify(connectionMock).enqueueMessage(message.linesIterator.toList)
        }
    }

    "sendAllEnqueuedMessages" should {

        "Add the prompt" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a message")
            val message = "This is a message"
            messageSender.sendAllEnqueuedMessages(actingPlayerMock)

            Then("It's sent with the prompt")
            verify(connectionMock).sendEnqueuedMessages(Seq(thePrompt), Seq())
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
            messageSender.sendAllEnqueuedMessages(actingPlayerMock, addMiniMap = true)

            Then("It's sent as three lines")
            verify(connectionMock).sendEnqueuedMessages(
                Seq("(12/20) fake-prompt (12/20)"),
                Seq(
                    "/-------------------\\",
                    "|                   |",
                    "|                   |",
                    "|                   |",
                    "|                   |",
                    "|                   |",
                    "|                   |",
                    "|         $BrightRedX$Reset         |",
                    "|         |         |",
                    "|         $Yellow#$Reset         |",
                    "|                   |",
                    "|                   |",
                    "|                   |",
                    "|                   |",
                    "\\-------------------/"
                ))
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
                verify(actingPlayerConnectionMock).enqueueMessage(Seq(message))
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
                verify(targetPlayerConnectionMock).enqueueMessage(Seq(message))
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
                verify(bystanderPlayerConnectionMock).enqueueMessage(Seq(message))
            }

            "Send message only to All Except Actor" in {

                Given("Characters in a room")
                val (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock) = setUpConnections

                When("Performing an act for All Except Actor")
                val message = "Message"
                messageSender.act("message", Always, Some(actingPlayerMock), Some(mediumItemMock), Some(targetPlayerMock), ToAllExceptActor, None)

                Then("It's sent only to Actor")
                verifyNoInteractions(actingPlayerConnectionMock)
                verify(targetPlayerConnectionMock).enqueueMessage(Seq(message))
                verify(bystanderPlayerConnectionMock).enqueueMessage(Seq(message))
            }

            "Send message only to Entire Room" in {

                Given("Characters in a room")
                val (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock) = setUpConnections

                When("Performing an act for Entire Room")
                val message = "Message"
                messageSender.act("message", Always, Some(actingPlayerMock), Some(mediumItemMock), Some(targetPlayerMock), ToEntireRoom, None)

                Then("It's sent only to Actor")
                verify(actingPlayerConnectionMock).enqueueMessage(Seq(message))
                verify(targetPlayerConnectionMock).enqueueMessage(Seq(message))
                verify(bystanderPlayerConnectionMock).enqueueMessage(Seq(message))
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
                verify(bystanderPlayerConnectionMock).enqueueMessage(Seq(message1, message2, message3))
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
