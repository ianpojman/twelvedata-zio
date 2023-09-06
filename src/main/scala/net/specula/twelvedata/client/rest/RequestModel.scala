package net.specula.twelvedata.client.rest

import zio.json.*
import zio.json.internal.Write

import java.time.LocalDate

case class ComplexDataRequestBody(
                                   symbols: List[String],
                                   intervals: List[String],
                                   methods: List[ComplexMethod],
                                   outputsize: Int = 30)

/** represents a named "method" in the complex_data API endpoint's request body. see https://twelvedata.com/docs#complex-data
 * This can be a timeseries request or a quote, etc. Each name */
case class ComplexMethod(commandName: String,
                         arguments: Map[String, String | Int])

/** wrapper class for deserialiation purposes */
case class ComplexMethodList(list: List[ComplexMethod])

object ComplexMethodList {
  def fromComplexMethods(c: ComplexMethod*): ComplexMethodList = ComplexMethodList(List(c:_*))
  /**
   * * In the json API (complex data request body), 'methods' is a special array that takes in either a string or an object, and it looks like this:
   * {{{
   *    [ "OPERATION", { "PARAMETER": "VALUE" }, "OPERATION", { "PARAMETER": "VALUE" } ]
   *    }}} *  for example (from twelvedata docs):
   * {{{
   *   "methods": [
   *"time_series",
   *{
   *"name": "ema",
   *"time_period": 12
   *},
   *"quote",
   *{
   *"name": "adx",
   *"order": "ASC"
   *}
   *]
   * }}}
   *
   * hence we have this custom JSON encoder that will emit the correct JSON that Twelvedata expects
   */
  implicit val encoder: JsonEncoder[ComplexMethodList] =
    new JsonEncoder[ComplexMethodList] {
      override def unsafeEncode(a: ComplexMethodList, indent: Option[Int], out: Write): Unit =
        out.write('[')
        a.list.foreach { m =>
          out.write('"' + m.commandName + '"')
          out.write(',')
          writeArgsAsJsonObject(m.arguments, out)
        }
        out.write(']')
    }

  private def writeArgsAsJsonObject(args: Map[String, String | Int], out: Write): Unit = {
    var first = true
    out.write('{')
    args.foreach { case (k, v) =>
      if (!first) out.write(',') else first = false

      out.write('"')
      out.write(k)
      out.write('"')
      out.write(':')
      v match {
        case s: String =>
          out.write('"')
          out.write(s)
          out.write('"')
        case i: Int =>
          out.write(i.toString)
      }
    }
    out.write('}')
  }
}

object ComplexMethod {
  def timeseries(): ComplexMethod =
//    ComplexMethod("time_series", Map("start_date" -> "2020-03-13")) //Map("name"->"ema", "time_period" -> 12)) // Map("start_date" -> startDate.toString, "end_date" -> endDate.toString))
    ComplexMethod("time_series", Map.empty) //Map("name"->"ema", "time_period" -> 12)) // Map("start_date" -> startDate.toString, "end_date" -> endDate.toString))

}

