package net.specula.twelvedata.client.util

object NetworkConfigurationUtil {

  def tryDisableIPv6(): Unit = {
    System.setProperty("java.net.preferIPv4Stack", "true")
    System.setProperty("java.net.preferIPv6Addresses", "false")

  }
}
