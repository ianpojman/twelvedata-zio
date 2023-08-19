package net.specula.twelvedata.client.rest

import zio.{Scope, ZIO}
import zio.json.*
import zio.test.*

object ComplexMethodTest extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("")(
      test("encode twelvedata complex_data methods field, emit correct json structure") {
        val method = ComplexMethod(
          "time_series",
          Map(
            "symbol" -> "AAPL",
            "interval" -> "1min",
            "outputsize" -> "1"
          )
        )
        import ComplexMethodList.*
        for {
          json <- ZIO.attempt(ComplexMethodList.fromComplexMethods(method).toJson)
        } yield assertTrue(json == """["time_series",{"symbol":"AAPL","interval":"1min","outputsize":"1"}]""")
      }
    )


}
