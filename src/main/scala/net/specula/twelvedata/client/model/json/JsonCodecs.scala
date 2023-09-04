package net.specula.twelvedata.client.model.json

import net.specula.twelvedata.client.model.{*}
import zio.json.internal.Write
import zio.json.{DeriveJsonEncoder, JsonDecoder, JsonEncoder}

object JsonCodecs {
  import net.specula.twelvedata.client.rest.ComplexMethodList.*
  import zio.json.*
  implicit val apiEODDecoder: JsonDecoder[ApiEOD] = DeriveJsonDecoder.gen
  implicit val priceBarDecoder: JsonDecoder[PriceBar] = DeriveJsonDecoder.gen
  implicit val priceBarEncoder: JsonDecoder[PriceBarSeries] = DeriveJsonDecoder.gen


  implicit val timeSeriesIntervalEncoder: JsonEncoder[TimeSeriesInterval] = new JsonEncoder[TimeSeriesInterval] {
    override def unsafeEncode(a: TimeSeriesInterval, indent: Option[Int], out: Write): Unit = {
      out.write("\"" + a.apiName + "\"")
    }
  }
  implicit val historicalDataRequestEncoder: JsonEncoder[TwelveDataComplexDataRequest] = DeriveJsonEncoder.gen
  implicit val dataElementDecoder: JsonDecoder[TwelveDataHistoricalDataResponse] = DeriveJsonDecoder.gen
  implicit val valueDecoder: JsonDecoder[ResponseElementValues] = DeriveJsonDecoder.gen
  implicit val indicatorDecoder: JsonDecoder[Indicator] = DeriveJsonDecoder.gen
  implicit val metaDecoder: JsonDecoder[ResponseElementMetadata] = DeriveJsonDecoder.gen
  implicit val TwelveDataResponseDecoder: JsonDecoder[TwelveDataHistoricalDataBatchResponse] = DeriveJsonDecoder.gen
}