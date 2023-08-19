package net.specula.twelvedata

import net.specula.twelvedata.client.model.*
import zio.http.Client
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}

package object client {
  type TickerToTimeSeriesItemMap = Map[String, PriceBarSeries]

  type TickerToApiPriceMap = Map[String, ApiPrice]



}
