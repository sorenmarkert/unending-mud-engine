package core.state

import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import core.commands.Commands.Command
import core.gameunit.{Mobile, NonPlayerCharacter, Room}
import core.state.StateActor.Execute
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar

import java.util.UUID
import scala.collection.mutable.{ListBuffer, Map as MMap}

class StateActorTest extends AnyWordSpec with MockitoSugar with Matchers with GivenWhenThen with BeforeAndAfterEach:

    given globalState: GlobalState = GlobalState()

    override def beforeEach(): Unit =
        globalState.clear()

    val testKit: BehaviorTestKit[StateActor.Message] = BehaviorTestKit(StateActor())

    val room: Room = Room("room")
    val character: NonPlayerCharacter = room.createNonPlayerCharacter("character")

    "StateActor" when {

        "Receiving an Execute message" should {

            "Add the message to the command queue" in {

                Given("A command")
                val commandMock = mock[Command]

                When("Sending an Execute message to the actor")
                val message = StateActor.Execute(commandMock, character, Nil)
                testKit.run(message)

                Then("The message is added to the command queue for the message's character")
                val commandQueue = getPrivateField[MMap[UUID, ListBuffer[Execute]]]("commandQueue")
                commandQueue should contain only (character.uuid -> List(message))
            }
        }

        "Receiving an Interrupt message" should {

            "Remove the character's ongoing timed commands" in {

                val timedCommandsOngoingMap = getPrivateField[MMap[Int, ListBuffer[Execute]]]("timedCommandsOngoingMap")
                val charactersInActionMap = getPrivateField[MMap[Mobile, Int]]("charactersInActionMap")

                Given("A command")
                val commandMock = mock[Command]
                val execute = StateActor.Execute(commandMock, character, Nil)
                val tickCount = 7
                timedCommandsOngoingMap(tickCount) = ListBuffer(execute)
                charactersInActionMap(character) = tickCount

                When("Sending a Destroy message to the actor")
                val message = StateActor.Interrupt(character)
                testKit.run(message)

                Then("The message is added to the command queue for the message's character")
                timedCommandsOngoingMap shouldBe empty
                charactersInActionMap shouldBe empty
            }
        }

        "Receiving a Destroy message" should {

            "Remove all the character's Execute messages from the command queue" in {

                val commandQueue = getPrivateField[MMap[UUID, ListBuffer[Execute]]]("commandQueue")

                Given("A command")
                val commandMock = mock[Command]
                val execute = StateActor.Execute(commandMock, character, Nil)
                commandQueue(character.uuid) = ListBuffer(execute)

                When("Sending a Destroy message to the actor")
                val message = StateActor.Destroy(character)
                testKit.run(message)

                Then("The message is added to the command queue for the message's character")
                commandQueue shouldBe empty
            }

            "Remove the character's ongoing timed commands" in {

                val timedCommandsOngoingMap = getPrivateField[MMap[Int, ListBuffer[Execute]]]("timedCommandsOngoingMap")
                val charactersInActionMap = getPrivateField[MMap[Mobile, Int]]("charactersInActionMap")

                Given("A command")
                val commandMock = mock[Command]
                val execute = StateActor.Execute(commandMock, character, Nil)
                val tickCount = 7
                timedCommandsOngoingMap(tickCount) = ListBuffer(execute)
                charactersInActionMap(character) = tickCount

                When("Sending a Destroy message to the actor")
                val message = StateActor.Destroy(character)
                testKit.run(message)

                Then("The message is added to the command queue for the message's character")
                timedCommandsOngoingMap shouldBe empty
                charactersInActionMap shouldBe empty
            }
        }

        "Receiving a Tick message" should {

            "Increment the tick counter" is pending

            "Execute all instant commands" is pending

            "Execute all timed commands and add them to the ongoing ones" is pending

            "Execute ending of all ongoing timed commands" is pending
        }
    }

    def getPrivateField[T](fieldName: String): T =
        val field = StateActor.getClass.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.get(null).asInstanceOf[T]