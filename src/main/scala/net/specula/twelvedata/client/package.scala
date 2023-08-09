package net.specula.twelvedata

import net.specula.twelvedata.client.model.{ApiPrice, DataElement, Indicator, ResponseMetadata, TwelveDataHistoricalDataRequest, TwelveDataHistoricalDataResponse, Value}
import zio.http.Client
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}

package object client {

  type TickerToApiPriceMap = Map[String, ApiPrice]

  /*
  {
    "": "curl 'https://api.twelvedata.com/time_series??symbol=AAPL&&interval=1day&outputsize=90&apikey=bf7d15e28ce44d62bf20f944901cc398&symbol=AAPL,MSFT'\n{",
    "AAPL": {
      "meta": {
        "symbol": "AAPL",
        "interval": "1day",
        "currency": "USD",
        "exchange_timezone": "America/New_York",
        "exchange": "NASDAQ",
        "mic_code": "XNGS",
        "type": "Common Stock"
      },
      "values": [
        {
          "datetime": "2023-04-28",
          "open": "168.49001",
          "high": "169.85001",
          "low": "167.88000",
          "close": "169.67999",
          "volume": "55209200"
        },
        {
          "datetime": "2023-04-27",
          "open": "165.19000",
          "high": "168.56000",
          "low": "165.19000",
          "close": "168.41000",
          "volume": "64902300"
  */

  case class TimeSeriesItems(values: List[TimeSeriesItem])

  case class TimeSeriesItem(datetime: String,
                            open: Double,
                            high: Double,
                            low: Double,
                            close: Double,
                            volume: Option[Double])


  object JsonCodecs {
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

    // ensure null gets translated to None, cf https://stackoverflow.com/a/72339993/1342121
//    implicit val handleNullDataFieldCustomDecoder: JsonDecoder[Option[List[DataElement]]] =     JsonDecoder[Option[List[DataElement]]].map {
//      x => if (x==null) None else x
//    }

  }
  type TickerToTimeSeriesItemMap = Map[String, TimeSeriesItems]
}
