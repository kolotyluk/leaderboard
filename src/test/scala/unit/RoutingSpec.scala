package unit

import akka.http.scaladsl.server.{Directives, Route}

/** =Base Unit Test=
  *
  */
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, Suite}

trait GenericRoutingSpec extends Matchers with Directives with ScalatestRouteTest { this: Suite ⇒
  val Ok = HttpResponse()
  val completeOk = complete(Ok)

  def echoComplete[T]: T ⇒ Route = { x ⇒ complete(x.toString) }
  def echoComplete2[T, U]: (T, U) ⇒ Route = { (x, y) ⇒ complete(s"$x $y") }
}

abstract class RoutingSpec extends UnitSpec with GenericRoutingSpec