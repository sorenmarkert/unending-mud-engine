package webapp

import org.scalajs.dom.document
import org.scalajs.dom.html.*

object DomUtils {

    def div(children: Element*): Div =
        val elem = document.createElement("div")
        children.foreach(elem.appendChild)
        elem.asInstanceOf[Div]
    
    def textarea(): TextArea =
        val elem = document.createElement("textarea")
        elem.asInstanceOf[TextArea]
}
