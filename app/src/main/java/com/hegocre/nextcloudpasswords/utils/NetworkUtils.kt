package com.hegocre.nextcloudpasswords.utils

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer

fun InetAddress.isDeviceLocalAddress(): Boolean {
    if (!this.isSiteLocalAddress || this !is Inet4Address) return false

    val targetInt = ByteBuffer.wrap(this.address).int

    NetworkInterface.getNetworkInterfaces().asSequence()
        .filter { !it.isLoopback && it.isUp }
        .flatMap { it.interfaceAddresses }
        .filter { it.address is Inet4Address }
        .forEach { iface ->
            val ifaceInt = ByteBuffer.wrap(iface.address.address).int
            val shift = 32 - iface.networkPrefixLength
            val mask = if (shift == 32) 0 else (-1 shl shift)

            // Compare the subnet bitmasks
            if ((targetInt and mask) == (ifaceInt and mask)) return true
        }

    return false
}