package com.getaltair.kairos.dashboard.auth

import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.getaltair.kairos.dashboard.auth.NetworkUtils")

/**
 * Returns the first site-local IPv4 address of a non-loopback, non-virtual,
 * active network interface, or `null` if no suitable address is found.
 */
fun getLocalIpAddress(): String? = try {
    NetworkInterface.getNetworkInterfaces()
        .asSequence()
        .filter { !it.isLoopback && it.isUp && !it.isVirtual }
        .flatMap { it.inetAddresses.asSequence() }
        .filterIsInstance<Inet4Address>()
        .filter { it.isSiteLocalAddress }
        .firstOrNull()
        ?.hostAddress
} catch (e: SocketException) {
    log.warn("Failed to enumerate network interfaces", e)
    null
}
