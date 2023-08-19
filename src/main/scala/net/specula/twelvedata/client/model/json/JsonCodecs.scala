package net.specula.twelvedata.client.model.json

import net.specula.twelvedata.client.model.{Symbol, *}
import zio.json.internal.Write
import zio.json.{DeriveJsonEncoder, JsonDecoder, JsonEncoder}

object JsonCodecs {
  import net.specula.twelvedata.client.rest.ComplexMethodList.*
  import zio.json.*
  implicit val priceBarDecoder: JsonDecoder[PriceBar] = DeriveJsonDecoder.gen
  implicit val priceBarEncoder: JsonDecoder[PriceBarSeries] = DeriveJsonDecoder.gen
  implicit val symbolEncoder: JsonEncoder[Symbol] = DeriveJsonEncoder.gen
  implicit val symbolDecoder: JsonDecoder[Symbol] = DeriveJsonDecoder.gen


  implicit val timeSeriesIntervalEncoder: JsonEncoder[TimeSeriesInterval] = new JsonEncoder[TimeSeriesInterval] {
    override def unsafeEncode(a: TimeSeriesInterval, indent: Option[Int], out: Write): Unit = {
      out.write("\"" + a.apiName + "\"")
    }
  }
  implicit val historicalDataRequestEncoder: JsonEncoder[TwelveDataHistoricalDataRequest] = DeriveJsonEncoder.gen
  implicit val dataElementDecoder: JsonDecoder[TwelveDataHistoricalDataResponseElement] = DeriveJsonDecoder.gen
  implicit val valueDecoder: JsonDecoder[ResponseElementValues] = DeriveJsonDecoder.gen
  implicit val indicatorDecoder: JsonDecoder[Indicator] = DeriveJsonDecoder.gen
  implicit val metaDecoder: JsonDecoder[ResponseElementMetadata] = DeriveJsonDecoder.gen
  implicit val TwelveDataResponseDecoder: JsonDecoder[TwelveDataHistoricalDataResponse] = DeriveJsonDecoder.gen
}