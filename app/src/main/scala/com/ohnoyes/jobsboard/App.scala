package com.ohnoyes.jobsboard

import scala.scalajs.js.annotation.* //JSExportTopLevel
import org.scalajs.dom.document

@JSExportTopLevel("OhNoYesApp")
class App {
    @JSExport
    def doSomething(container: String) =
        document.getElementById(container).innerHTML = 
            "Hello, from Scala.js!"
            // In JS: document.getElementById(container).innerHTML = "Hello, world!"}
}
