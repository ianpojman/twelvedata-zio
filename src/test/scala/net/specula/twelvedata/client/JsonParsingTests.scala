package net.specula.twelvedata.client

import org.specs2.mutable.Specification

class JsonParsingTests extends Specification {
  "json parsing" should {
    "parse time series response" in {
      import zio.json.*
      import zio.*
      import TimeSeriesCodecs.*

      val json = """{"SPX":{"meta":{"symbol":"SPX","interval":"1h","currency":"USD","exchange_timezone":"America/New_York","exchange":"NYSE","mic_code":"XNYS","type":"Index"},"values":[{"datetime":"2023-05-12 15:30:00","open":"4111.64990","high":"4124.43994","low":"4111.64990","close":"4123.85010","volume":"262548323"},{"datetime":"2023-05-12 14:30:00","open":"4103.27979","high":"4112.33008","low":"4099.45996","close":"4111.68018","volume":"230960186"},{"datetime":"2023-05-12 13:30:00","open":"4111.04004","high":"4113.74023","low":"4099.12012","close":"4104.33984","volume":"1460761459"},{"datetime":"2023-05-12 12:30:00","open":"4109.02002","high":"4114.43994","low":"4101.43994","close":"4111.04004","volume":"205952586"},{"datetime":"2023-05-12 11:30:00","open":"4116.85010","high":"4123.25000","low":"4108.43994","close":"4109.02979","volume":"222100594"},{"datetime":"2023-05-12 10:30:00","open":"4127.95996","high":"4127.95996","low":"4116.04004","close":"4116.89990","volume":"774400592"},{"datetime":"2023-05-12 09:30:00","open":"4138.54004","high":"4143.74023","low":"4126.87988","close":"4127.97998","volume":"376192749"},{"datetime":"2023-05-11 15:30:00","open":"4128.70996","high":"4131.93994","low":"4123.89990","close":"4130.00977","volume":"260390553"},{"datetime":"2023-05-11 14:30:00","open":"4123.91016","high":"4132.79980","low":"4123.14014","close":"4128.81006","volume":"226513212"},{"datetime":"2023-05-11 13:30:00","open":"4123.06006","high":"4127.10986","low":"4112.02002","close":"4123.93018","volume":"196723641"},{"datetime":"2023-05-11 12:30:00","open":"4126.79980","high":"4130.22998","low":"4121.87988","close":"4123.02002","volume":"190000778"},{"datetime":"2023-05-11 11:30:00","open":"4123.56982","high":"4130.87012","low":"4122.04980","close":"4126.70996","volume":"235195300"},{"datetime":"2023-05-11 10:30:00","open":"4117.43994","high":"4125.77002","low":"4110.39990","close":"4123.56006","volume":"976629835"},{"datetime":"2023-05-11 09:30:00","open":"4132.24023","high":"4132.24023","low":"4109.29004","close":"4117.37012","volume":"430478682"},{"datetime":"2023-05-10 15:30:00","open":"4141.89014","high":"4147.31982","low":"4135.85010","close":"4137.10010","volume":"261857323"},{"datetime":"2023-05-10 14:30:00","open":"4119.77979","high":"4145.56006","low":"4119.77979","close":"4141.91016","volume":"251611333"},{"datetime":"2023-05-10 13:30:00","open":"4102.06006","high":"4121.87988","low":"4098.91992","close":"4119.75000","volume":"221691098"},{"datetime":"2023-05-10 12:30:00","open":"4123.52979","high":"4124.60010","low":"4099.93994","close":"4102.16992","volume":"221427320"},{"datetime":"2023-05-10 11:30:00","open":"4129.02002","high":"4133.70020","low":"4119.25000","close":"4123.58008","volume":"213422131"},{"datetime":"2023-05-10 10:30:00","open":"4131.08008","high":"4134.89014","low":"4112.41016","close":"4129.04004","volume":"300458345"},{"datetime":"2023-05-10 09:30:00","open":"4143.74023","high":"4154.27979","low":"4125.37012","close":"4131.04980","volume":"423789911"},{"datetime":"2023-05-09 15:30:00","open":"4125.00000","high":"4126.68994","low":"4118.12988","close":"4119.25000","volume":"248515866"},{"datetime":"2023-05-09 14:30:00","open":"4127.72998","high":"4128.72998","low":"4121.47021","close":"4124.99023","volume":"203480547"},{"datetime":"2023-05-09 13:30:00","open":"4123.58008","high":"4130.35010","low":"4122.43994","close":"4127.72021","volume":"181713080"},{"datetime":"2023-05-09 12:30:00","open":"4118.58984","high":"4124.41016","low":"4118.52002","close":"4123.60986","volume":"164148616"},{"datetime":"2023-05-09 11:30:00","open":"4122.31982","high":"4126.31006","low":"4118.41016","close":"4118.58984","volume":"197525074"},{"datetime":"2023-05-09 10:30:00","open":"4122.89014","high":"4126.58984","low":"4119.25000","close":"4122.29004","volume":"1440572563"},{"datetime":"2023-05-09 09:30:00","open":"4124.25000","high":"4127.68994","low":"4116.64990","close":"4122.83984","volume":"378128184"},{"datetime":"2023-05-08 15:30:00","open":"4135.83008","high":"4141.02979","low":"4135.06982","close":"4137.89990","volume":"238991069"},{"datetime":"2023-05-08 14:30:00","open":"4137.97998","high":"4142.29980","low":"4134.75977","close":"4135.85010","volume":"220200613"}],"status":"ok"},"AAPL":{"meta":{"symbol":"AAPL","interval":"1h","currency":"USD","exchange_timezone":"America/New_York","exchange":"NASDAQ","mic_code":"XNGS","type":"Common Stock"},"values":[{"datetime":"2023-05-12 15:30:00","open":"171.91000","high":"172.62000","low":"171.90500","close":"172.56000","volume":"5265382"},{"datetime":"2023-05-12 14:30:00","open":"171.33000","high":"171.92999","low":"171.17999","close":"171.90010","volume":"5318972"},{"datetime":"2023-05-12 13:30:00","open":"171.77000","high":"171.97501","low":"171.00000","close":"171.33000","volume":"4597136"},{"datetime":"2023-05-12 12:30:00","open":"171.92999","high":"172.19000","low":"171.31500","close":"171.77499","volume":"4645213"},{"datetime":"2023-05-12 11:30:00","open":"172.48500","high":"172.72000","low":"171.92999","close":"171.94501","volume":"4830042"},{"datetime":"2023-05-12 10:30:00","open":"172.69000","high":"172.97501","low":"172.33000","close":"172.48000","volume":"5603315"},{"datetime":"2023-05-12 09:30:00","open":"173.67999","high":"174.06000","low":"172.50999","close":"172.69000","volume":"9900308"},{"datetime":"2023-05-11 15:30:00","open":"174.07500","high":"174.14999","low":"173.50000","close":"173.81000","volume":"4250305"},{"datetime":"2023-05-11 14:30:00","open":"173.99989","high":"174.59000","low":"173.86000","close":"174.09000","volume":"6007254"},{"datetime":"2023-05-11 13:30:00","open":"173.47000","high":"174.00000","low":"173.06000","close":"173.99010","volume":"4089255"},{"datetime":"2023-05-11 12:30:00","open":"173.67000","high":"174.02499","low":"173.38000","close":"173.47000","volume":"4415832"},{"datetime":"2023-05-11 11:30:00","open":"173.69501","high":"173.91499","low":"173.25999","close":"173.66000","volume":"5325734"},{"datetime":"2023-05-11 10:30:00","open":"172.67000","high":"173.70000","low":"172.30000","close":"173.70000","volume":"6827208"},{"datetime":"2023-05-11 09:30:00","open":"173.85001","high":"173.96001","low":"172.17000","close":"172.67000","volume":"11552802"},{"datetime":"2023-05-10 15:30:00","open":"173.64000","high":"173.91000","low":"173.34000","close":"173.55499","volume":"5349524"},{"datetime":"2023-05-10 14:30:00","open":"172.62500","high":"173.84000","low":"172.61000","close":"173.63000","volume":"6304643"},{"datetime":"2023-05-10 13:30:00","open":"172.02000","high":"172.84000","low":"171.89999","close":"172.62000","volume":"4067612"},{"datetime":"2023-05-10 12:30:00","open":"173.17210","high":"173.30000","low":"171.98000","close":"172.03000","volume":"4412739"},{"datetime":"2023-05-10 11:30:00","open":"173.30000","high":"173.83000","low":"172.78999","close":"173.17751","volume":"5334991"},{"datetime":"2023-05-10 10:30:00","open":"173.05190","high":"173.42000","low":"172.28999","close":"173.29010","volume":"6608451"},{"datetime":"2023-05-10 09:30:00","open":"173.02000","high":"174.03000","low":"172.55000","close":"173.05000","volume":"13192537"},{"datetime":"2023-05-09 15:30:00","open":"171.97000","high":"172.23500","low":"171.60001","close":"171.78000","volume":"6390735"},{"datetime":"2023-05-09 14:30:00","open":"172.27000","high":"172.35760","low":"171.85001","close":"171.96500","volume":"3458267"},{"datetime":"2023-05-09 13:30:00","open":"172.21001","high":"172.33980","low":"171.96001","close":"172.27000","volume":"3038422"},{"datetime":"2023-05-09 12:30:00","open":"172.05499","high":"172.48000","low":"172.02000","close":"172.20500","volume":"3808782"},{"datetime":"2023-05-09 11:30:00","open":"172.28999","high":"172.39990","low":"171.81000","close":"172.05910","volume":"4798870"},{"datetime":"2023-05-09 10:30:00","open":"172.53000","high":"172.64000","low":"172.14500","close":"172.28040","volume":"5427003"},{"datetime":"2023-05-09 09:30:00","open":"173.05000","high":"173.53999","low":"172.25999","close":"172.52000","volume":"13477160"},{"datetime":"2023-05-08 15:30:00","open":"173.39999","high":"173.67999","low":"173.37000","close":"173.52499","volume":"4325310"},{"datetime":"2023-05-08 14:30:00","open":"173.59500","high":"173.80000","low":"173.21001","close":"173.40500","volume":"4103527"}],"status":"ok"}}"""

      val res: Either[String, MultiTickerTimeSeriesResponseJson] = json.fromJson[MultiTickerTimeSeriesResponseJson]
      res must beRight
    }
  }


}
