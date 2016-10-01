package spreadsheet

import spreadsheet.ui.Spreadsheet
import spreadsheet._

import scala.scalajs.js
import org.scalajs.dom
import dom._
import woot._
import upickle.default._

object Main extends js.JSApp {

  def main(): Unit = {
    val cellsElem = document.getElementById("cells")
    val protocol = if( window.location.protocol.startsWith("https") ) { "wss" } else { "ws" }
    val url = s"${protocol}://${dom.window.location.host}/ws/edit/abc"
    val ws = new dom.WebSocket(url)
    def broadcastCellOperation(cellOp: SpreadSheetOp): Unit = {
      println(s"broadcasting $cellOp")
      ws.send( write(cellOp) )
    }
    var spreadSheet: Spreadsheet = null
    ws.onmessage = { x: MessageEvent =>
      read[ClientMessage](x.data.toString) match {
        case ClientMessage(Some(sp),_) =>
          println(s"site id = ${sp.siteId}")
          spreadSheet = Spreadsheet(sp, cellsElem, broadcastCellOperation)
        case ClientMessage(_,Some(operation)) =>
          if(operation.from != spreadSheet.siteId) { //@TODO this filtering should be done on the server
            println(s"received operation $operation")
            spreadSheet.receiveRemoteOperation(operation)
          }
      }
    }

  }

}
