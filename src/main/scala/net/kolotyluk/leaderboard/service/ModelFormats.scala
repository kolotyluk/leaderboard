package net.kolotyluk.leaderboard.service


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

/**
  * @author rleibman
  */
trait ModelFormats
  extends DefaultJsonProtocol
    with SprayJsonSupport {
  //implicit val dictEntryformats = jsonFormat3(DictEntry)
}