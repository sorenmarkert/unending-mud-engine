package core.util

import core.gameunit.*
import core.util.MessagingUtils.{joinOrElse, unitDisplay}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, GivenWhenThen}
import org.scalatestplus.mockito.MockitoSugar

class MessagingUtilsTest extends AnyWordSpec with MockitoSugar with GivenWhenThen with Matchers:

    val playerMock  = mock[PlayerCharacter]
    val playerName  = "playerName"
    val playerTitle = "playerTitle"
    when(playerMock.name).thenReturn(playerName)
    when(playerMock.title).thenReturn(playerTitle)

    val unitMock  = mock[GameUnit]
    val unitTitle = "unitTitle"
    when(unitMock.title).thenReturn(unitTitle)

    "joinOrElse" should {

        "Return default on empty input" in {

            Given("A default value")
            val defaultValue = "default"

            When("Joining an empty list")
            val result = joinOrElse(List(), "separator", defaultValue)

            Then("The default value should be returned")
            result shouldBe defaultValue
        }

        "Return a single input" in {

            Given("A single input")
            val input       = "input"
            val singleInput = List(input)

            When("Joining the input")
            val result = joinOrElse(singleInput, "separator", "default")

            Then("That input should be returned")
            result shouldBe input
        }

        "Return multiple inputs joined with the separator" in {

            Given("Multiple inputs and a separator")
            val input1         = "input1"
            val input2         = "input2"
            val input3         = "input3"
            val multipleInputs = List(input1, input2, input3)
            val separator      = "separator"

            When("Joining the inputs")
            val result = joinOrElse(multipleInputs, separator, "default")

            Then("The joined inputs with separator are returned")
            result shouldBe (input1 + separator + input2 + separator + input3)
        }
    }

    "unitDisplay" should {

        "Return player name and title" in {

            Given("A player character with name and title")

            When("Displaying a player")
            val result = unitDisplay(playerMock)

            Then("The name and title are returned")
            result shouldBe s"$playerName $playerTitle"
        }

        "Return player name" in {

            Given("A player character with name and title")

            When("Displaying a player without the title")
            val result = unitDisplay(playerMock, false)

            Then("Only the name is returned")
            result shouldBe playerName
        }

        "Return unit title" in {

            Given("A unit other than a player character that has a title")

            When("Displaying the unit")
            val result = unitDisplay(unitMock, false)

            Then("Its title is returned")
            result shouldBe unitTitle
        }
    }
