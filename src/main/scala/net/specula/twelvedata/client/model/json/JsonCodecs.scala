package net.specula.twelvedata.client.model.json

import net.specula.twelvedata.client.model.{Symbol, *}
import zio.json.{DeriveJsonEncoder, JsonDecoder, JsonEncoder}

object JsonCodecs {
  import zio.json.*
  implicit val timeSeriesItemDecoder: JsonDecoder[TimeSeriesItem] = DeriveJsonDecoder.gen
  implicit val timeSeriesItemsDecoder: JsonDecoder[TimeSeriesItems] = DeriveJsonDecoder.gen
  implicit val symbolEncoder: JsonEncoder[Symbol] = DeriveJsonEncoder.gen
  implicit val symbolDecoder: JsonDecoder[Symbol] = DeriveJsonDecoder.gen
  implicit val historicalDataRequestDecoder: JsonDecoder[TwelveDataHistoricalDataRequest] = DeriveJsonDecoder.gen
  implicit val historicalDataRequestEncoder: JsonEncoder[TwelveDataHistoricalDataRequest] = DeriveJsonEncoder.gen
  implicit val dataElementDecoder: JsonDecoder[DataElement] = DeriveJsonDecoder.gen
  implicit val valueDecoder: JsonDecoder[Value] = DeriveJsonDecoder.gen
  implicit val indicatorDecoder: JsonDecoder[Indicator] = DeriveJsonDecoder.gen
  implicit val metaDecoder: JsonDecoder[ResponseMetadata] = DeriveJsonDecoder.gen
  implicit val TwelveDataResponseDecoder: JsonDecoder[TwelveDataHistoricalDataResponse] = DeriveJsonDecoder.gen
}