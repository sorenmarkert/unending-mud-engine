package core.commands

import akka.actor.typed.ActorSystem
import core.MessageSender
import core.commands.Commands.InstantCommand
import core.gameunit.NonPlayerCharacter
import core.state.StateActor
import core.storage.Storage
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{doNothing, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen, OptionValues}
import org.scalatestplus.mockito.MockitoSugar

class CommandsTest extends AnyWordSpec with MockitoSugar with GivenWhenThen with Matchers with BeforeAndAfterEach with OptionValues:

    given actorSystemMock: ActorSystem[StateActor.Message] = mock[ActorSystem[StateActor.Message]]

    given messageSenderMock: MessageSender = mock[MessageSender]

    given Storage = mock[Storage] // TODO: remove later

    given BasicCommands = BasicCommands()

    given CombatCommands = CombatCommands()

    given UtilityCommands = UtilityCommands()

    given equipmentCommands: EquipmentCommands = EquipmentCommands()

    val commands = Commands()

    val characterMock = mock[NonPlayerCharacter]

    var executeCaptor: ArgumentCaptor[StateActor.Execute] = null

    override def beforeEach() =
        executeCaptor = ArgumentCaptor.forClass(classOf[StateActor.Execute])
        doNothing().when(actorSystemMock).tell(executeCaptor.capture())

    "executeCommandAtNextTick" when {

        "Calling the actor system" should {

            "Use the emptyInput command" in {

                Given("An empty command string")
                val commandString = ""

                When("Executing the command")
                commands.executeCommandAtNextTick(characterMock, commandString)

                Then("The correct Execute is sent to the actor system")
                val capturedExecute = executeCaptor.getValue
                capturedExecute.character shouldBe characterMock
                capturedExecute.argument shouldBe Nil

                capturedExecute.command shouldBe a[InstantCommand]
                val instantCommand: InstantCommand = capturedExecute.command.asInstanceOf[InstantCommand]
                instantCommand.func(characterMock, null)
                verify(messageSenderMock).sendMessageToCharacter(characterMock, "")
            }

            "Use the unknown command" in {

                Given("An invalid command string")
                val commandString = "not-a-valid-command"

                When("Executing the command")
                commands.executeCommandAtNextTick(characterMock, commandString)

                Then("The correct Execute is sent to the actor system")
                val capturedExecute = executeCaptor.getValue
                capturedExecute.character shouldBe characterMock
                capturedExecute.argument shouldBe Nil

                capturedExecute.command shouldBe a[InstantCommand]
                val instantCommand: InstantCommand = capturedExecute.command.asInstanceOf[InstantCommand]
                instantCommand.func(characterMock, null)
                verify(messageSenderMock).sendMessageToCharacter(characterMock, "What's that?")
            }

            "Expand the command word" in {

                Given("A valid command string")
                val commandString = "inven"
                val expectedCommandWords = List("inventory")

                When("Executing the command")
                commands.executeCommandAtNextTick(characterMock, commandString)

                Then("The correct Execute is sent to the actor system")
                val capturedExecute = executeCaptor.getValue
                capturedExecute.character shouldBe characterMock
                capturedExecute.argument shouldBe expectedCommandWords
                capturedExecute.command shouldBe commands.commandsByWord("inventory")
            }

            "Expand the command word and include arguments" in {

                Given("A valid command string with arguments")
                val commandString = "g hat from bag"
                val expectedCommandWords = "get hat from bag".split(' ').toList

                When("Executing the command")
                commands.executeCommandAtNextTick(characterMock, commandString)

                Then("The correct Execute is sent to the actor system")
                val capturedExecute = executeCaptor.getValue
                capturedExecute.character shouldBe characterMock
                capturedExecute.argument shouldBe expectedCommandWords
                capturedExecute.command shouldBe commands.commandsByWord("get")
            }
        }
    }
