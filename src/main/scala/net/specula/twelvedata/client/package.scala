package net.specula.twelvedata

import net.specula.twelvedata.client.model.twelvedata.ApiPrice
import zio.json.{DeriveJsonDecoder, JsonDecoder}

package object client {

  type PriceByTickerMap = Map[String, ApiPrice]

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


  object TimeSeriesCodecs {
    implicit val timeSeriesItemDecoder: JsonDecoder[TimeSeriesItem] = DeriveJsonDecoder.gen
    implicit val timeSeriesItemsDecoder: JsonDecoder[TimeSeriesItems] = DeriveJsonDecoder.gen
  }
  type MultiTickerTimeSeriesResponse = Map[String, TimeSeriesItems]
  type MultiTickerTimeSeriesResponseJson = Map[String, TimeSeriesItems]
}
