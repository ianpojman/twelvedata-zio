package net.specula.twelvedata.client.model


/*

1. query number 1: OPEX

url: https://api.twelvedata.com/options/expiration?symbol=AAPL&apikey=demo&source=docs

example response:
{"meta":{"symbol":"AAPL","name":"Apple Inc","currency":"USD","exchange":"NASDAQ","mic_code":"XNGS","exchange_timezone":"America/New_York"},
  "dates":["2023-10-06","2023-10-13","2023-10-20","2023-10-27","2023-11-03","2023-11-10","2023-11-17","2023-12-15","2024-01-19","2024-02-16","2024-03-15","2024-04-19","2024-06-21","2024-09-20","2024-12-20","2025-01-17","2025-06-20","2025-12-19","2026-01-16"]}


 */
/*

2. query number 2: OPTION CHAIN

url:  https://api.twelvedata.com/options/chain?symbol=AAPL&expiration_date=2022-01-21&apikey=demo&source=docs

 example response:
{
  "meta": {
    "symbol": "AAPL",
    "name": "Apple Inc",
    "currency": "USD",
    "exchange": "NASDAQ",
    "mic_code": "XNGS",
    "exchange_timezone": "America/New_York"
  },
  "calls": [
    {
      "contract_name": "AAPL Option Call 21-01-2022 25",
      "option_id": "AAPL220121C00025000",
      "last_trade_date": "2021-12-30 15:01:11",
      "strike": 25,
      "last_price": 154.3,
      "bid": 148,
      "ask": 152.5,
      "change": 0,
      "percent_change": 0,
      "volume": 2,
      "open_interest": 12,
      "implied_volatility": 46.156252788085936,
      "in_the_money": true
    },
 */
case class Meta(symbol: String, name: String, currency: String, exchange: String, mic_code: String, exchange_timezone: String)

case class Call(contract_name: String, option_id: String, last_trade_date: String, strike: Double, last_price: Double, bid: Double, ask: Double, change: Double, percent_change: Double, volume: Int, open_interest: Int, implied_volatility: Double, in_the_money: Boolean)
case class Put(contract_name: String, option_id: String, last_trade_date: String, strike: Double, last_price: Double, bid: Double, ask: Double, change: Double, percent_change: Double, volume: Int, open_interest: Int, implied_volatility: Double, in_the_money: Boolean)

case class OptionData(meta: Meta, calls: List[Call], puts: List[Put])

case class OptionExpirations(meta: Meta, dates: List[String])