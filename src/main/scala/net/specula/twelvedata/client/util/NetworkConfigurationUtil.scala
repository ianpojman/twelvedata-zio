package net.specula.twelvedata.client.util

object NetworkConfigurationUtil {

  def trDisableIPv6(): Unit = {
    System.setProperty("java.net.preferIPv4Stack", "true")
  }
}
