package core

import core.ActVisibility.Always
import core.connection.WebSocketConnection
import core.gameunit.*
import core.gameunit.Gender.{GenderFemale, GenderMale}
import core.state.GlobalState
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.*
import org.scalatest.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class MessageSenderTest extends AnyWordSpec with MockitoSugar with Matchers with GivenWhenThen with BeforeAndAfterEach:

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
        when(targetPlayerMock.outside).thenReturn(roomMock)
        when(bystanderPlayerMock.outside).thenReturn(roomMock)
        when(roomMock.mobiles).thenReturn(Seq(actingPlayerMock, targetPlayerMock, bystanderPlayerMock))

    "sendMessageToRoomOf" should {

        "call sendMessageToCharacter on all characters in the same room" in {

            Given("Some characters in a room and a message")
            val message = "message"
            val messageSenderSpy = setupSendMessageToCharacterAndSpy(message)

            When("Sending a message to the room of a character")
            val result = messageSenderSpy.sendMessageToRoomOf(actingPlayerMock, message)

            Then("Only bystanders get sent the message")
            result shouldBe Seq(actingPlayerMock, targetPlayerMock, bystanderPlayerMock)
            verify(messageSenderSpy).sendMessageToCharacter(actingPlayerMock, message)
            verify(messageSenderSpy).sendMessageToCharacter(targetPlayerMock, message)
            verify(messageSenderSpy).sendMessageToCharacter(bystanderPlayerMock, message)

        }
    }

    "sendMessageToBystandersOf" should {

        "call sendMessageToCharacter on all given characters" in {

            Given("Some characters in a room and a message")
            val message = "message"
            val messageSenderSpy = setupSendMessageToCharacterAndSpy(message)

            When("Sending a message to bystanders of a character")
            val result = messageSenderSpy.sendMessageToBystandersOf(actingPlayerMock, message)

            Then("Only bystanders get sent the message")
            result shouldBe Seq(targetPlayerMock, bystanderPlayerMock)
            verify(messageSenderSpy, never).sendMessageToCharacter(actingPlayerMock, message)
            verify(messageSenderSpy).sendMessageToCharacter(targetPlayerMock, message)
            verify(messageSenderSpy).sendMessageToCharacter(bystanderPlayerMock, message)

        }
    }

    "sendMessageToCharacters" should {

        "call sendMessageToCharacter on all given characters" in {

            Given("Some characters and a message")
            val characters = Seq(actingPlayerMock, targetPlayerMock, bystanderPlayerMock)
            val message = "message"
            val messageSenderSpy = setupSendMessageToCharacterAndSpy(message)

            When("Sending a message to all the characters")
            val result = messageSenderSpy.sendMessageToCharacters(characters, message)

            Then("All characters get sent the message")
            result shouldBe characters
            verify(messageSenderSpy).sendMessageToCharacter(actingPlayerMock, message)
            verify(messageSenderSpy).sendMessageToCharacter(targetPlayerMock, message)
            verify(messageSenderSpy).sendMessageToCharacter(bystanderPlayerMock, message)

        }
    }

    "sendMessageToCharacter" should {

        "Capitalize the message" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a message starting with a lower case letter")
            messageSender.sendMessageToCharacter(actingPlayerMock, "message")

            Then("The message is sent capitalized")
            verify(connectionMock).enqueueMessage(Seq("Message"))
        }

        "Retain line breaks of input" in {

            Given("A player with a connection")
            val connectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)

            When("Sending a message with line breaks")
            val message = "This message\nhas two\nline breaks."
            messageSender.sendMessageToCharacter(actingPlayerMock, message)

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
                    "|         <span style=\"color:red;font-weight:bold;\">X</span>         |",
                    "|         |         |",
                    "|         <span style=\"color:yellow\">#</span>         |",
                    "|                   |",
                    "|                   |",
                    "|                   |",
                    "|                   |",
                    "\\-------------------/"
                ))
        }

        "Send to controller of an NPC" is pending
    }

    "act" should {

        "Replace formatter codes with units and verbs" in {

            Given("Characters in a room")
            val (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock) = setUpConnections
            when(actingPlayerMock.name).thenReturn("actorName")
            when(actingPlayerMock.gender).thenReturn(GenderFemale)
            when(targetPlayerMock.name).thenReturn("targetName")
            when(targetPlayerMock.gender).thenReturn(GenderMale)
            when(mediumItemMock.name).thenReturn("mediumItemName")

            When("Performing an act with unit and verb formatters")
            val message = "$1N $[wave|waves] at $3N with $2a $2N, before $1e $[break|breaks] $2e over $3m, $1s best $1t."
            messageSender.act(
                message, Always,
                Some(actingPlayerMock), Some(mediumItemMock), Some(targetPlayerMock),
                Some("enemy"))

            Then("It's sent only to Actor")
            val expectedMessageForActor =
                """You wave at targetName with a mediumItemName,
                  |before you break it over him, your best enemy.""".stripMargin.linesIterator.toSeq
            val expectedMessageForTarget =
                """ActorName waves at you with a mediumItemName,
                  |before she breaks it over you, her best enemy.""".stripMargin.linesIterator.toSeq
            val expectedMessageForBystander =
                """ActorName waves at targetName with a
                  |mediumItemName, before she breaks it over him, her
                  |best enemy.""".stripMargin.linesIterator.toSeq
            verify(actingPlayerConnectionMock).enqueueMessage(expectedMessageForActor)
            verify(targetPlayerConnectionMock).enqueueMessage(expectedMessageForTarget)
            verify(bystanderPlayerConnectionMock).enqueueMessage(expectedMessageForBystander)
        }
    }

    private def setupSendMessageToCharacterAndSpy(message: String): MessageSender =
        val messageSenderSpy = spy(classOf[MessageSender])
        doReturn(Seq(actingPlayerMock)).when(messageSenderSpy).sendMessageToCharacter(actingPlayerMock, message)
        doReturn(Seq(targetPlayerMock)).when(messageSenderSpy).sendMessageToCharacter(targetPlayerMock, message)
        doReturn(Seq(bystanderPlayerMock)).when(messageSenderSpy).sendMessageToCharacter(bystanderPlayerMock, message)
        messageSenderSpy

    private def setUpConnectionMockOnPlayer(playerMock: PlayerCharacter): WebSocketConnection =
        val connectionMock = mock[WebSocketConnection]
        when(connectionMock.substituteColourCodes(any())).thenCallRealMethod()
        when(playerMock.connection).thenReturn(connectionMock)
        connectionMock

    private def setUpConnections: (WebSocketConnection, WebSocketConnection, WebSocketConnection) =
        val actingPlayerConnectionMock = setUpConnectionMockOnPlayer(actingPlayerMock)
        val targetPlayerConnectionMock = setUpConnectionMockOnPlayer(targetPlayerMock)
        val bystanderPlayerConnectionMock = setUpConnectionMockOnPlayer(bystanderPlayerMock)
        (actingPlayerConnectionMock, targetPlayerConnectionMock, bystanderPlayerConnectionMock)
