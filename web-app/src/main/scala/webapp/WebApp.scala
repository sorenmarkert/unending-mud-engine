package webapp

import org.scalajs.dom.document
import org.scalajs.dom.html.*
import webapp.DomUtils.*

object WebApp {

    val contentTextArea = textarea()

    val form: Div = div(
        contentTextArea,
        )

    @main def start =
        document.body.appendChild(form)
        contentTextArea.value = "asdasd"

}
