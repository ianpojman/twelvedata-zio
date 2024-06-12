package net.specula

import net.specula.twelvedata.client.TwelveDataConfig
import zio.http.Client

package object twelvedata {
  type TwelveDataClient = TwelveDataConfig & Client
}
