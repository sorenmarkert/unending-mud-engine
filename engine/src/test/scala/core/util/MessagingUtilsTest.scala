package core.util

import core.connection.WebSocketConnection
import core.gameunit.*
import core.util.MessagingUtils.*
import org.mockito.Mockito.when
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class MessagingUtilsTest extends AnyWordSpec with MockitoSugar with GivenWhenThen with Matchers:

    val playerMock = mock[PlayerCharacter]
    val playerName = "playerName"
    val playerTitle = "playerTitle"
    when(playerMock.name).thenReturn(playerName)
    when(playerMock.title).thenReturn(playerTitle)

    val unitMock = mock[GameUnit]
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
            val input = "input"
            val singleInput = List(input)

            When("Joining the input")
            val result = joinOrElse(singleInput, "separator", "default")

            Then("That input should be returned")
            result shouldBe input
        }

        "Return multiple inputs joined with the separator" in {

            Given("Multiple inputs and a separator")
            val input1 = "input1"
            val input2 = "input2"
            val input3 = "input3"
            val multipleInputs = List(input1, input2, input3)
            val separator = "separator"

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

    "collapseNames" should {

        "Collapse duplicated strings into singles with counts" in {

            Given("A list with a duplicated string")
            val duplicates = List("duplicated", "duplicated")

            When("Displaying a player")
            val result = collapseDuplicates(duplicates)

            Then("The name and title are returned")
            result should contain only "[x2] duplicated"
        }

        "Maintain order of first occurrences" in {

            Given("A list with a duplicated string")
            val duplicates = List("first", "second", "second", "first", "third", "second", "third", "third")

            When("Displaying a player")
            val result = collapseDuplicates(duplicates)

            Then("The name and title are returned")
            result should contain inOrderOnly("[x2] first", "[x3] second", "[x3] third")
        }

    }

    "groupedIgnoringColourCodes" should {

        "Wrap lines longer than textWidth" in {

            Given("A message longer than 2x 50 characters")
            val message1 = "This message string is exactly 49 characters long"
            val message2 = "and it continues in this string which is also 49"
            val message3 = "and then it ends here."

            When("Grouping the message")
            val result = groupedIgnoringColourCodes(s"$message1 $message2 $message3", 50)

            Then("It's grouped as three lines")
            result.toSeq shouldBe Seq(message1, message2, message3)
        }

        "Not count colour codes when wrapping words" in {

            Given("A long message with colour codes")
            val message1 = "This message $BrightMagentastring has colour codes and it"
            val message2 = "continues in the next $Reset string."

            When("Grouping the message")
            val result = groupedIgnoringColourCodes(message1 + " " + message2, 50)

            Then("Word wrap counts the message without formatting")
            result.toSeq shouldBe Seq(message1, message2)
        }

        "Replace space formatters" in {

            Given("A long message with space formatters")
            val message1 = "This message has$s10space formatters and it"
            val message2 = "continues in$s10the next string."

            When("Grouping the message")
            val result = groupedIgnoringColourCodes(message1 + " " + message2, 50)

            Then("Word wrap counts the message without formatting")
            val expectedMessage1 = "This message has          space formatters and it"
            val expectedMessage2 = "continues in          the next string."
            result.toSeq shouldBe Seq(expectedMessage1, expectedMessage2)
        }
    }

    "substituteColours" should {

        "Replace colour codes" in {

            Given("A message with colour codes")
            val message = "Here $Greenare so$BrightMagentame colo$Reseturs"

            When("Substituting the colour codes")
            val result = substituteColours(message, WebSocketConnection(null).substituteColourCodes)

            Then("The colour codes have been replace with connection specific style formatting")
            result shouldBe "Here <span style=\"color:green\">are so<span style=\"color:magenta;font-weight:bold;\">me colo</span>urs"
        }
    }
