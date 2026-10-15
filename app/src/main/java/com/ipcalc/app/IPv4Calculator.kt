package com.ipcalc.app

import java.text.NumberFormat
import java.util.Locale

/**
 * Pure calculation engine for IPv4 subnet math.
 * All operations work on Long (32-bit unsigned) to avoid sign issues with Java's signed int.
 */
object IPv4Calculator {

    // ─────────────────────────────────────────────────────────────────────────
    // Result data class
    // ─────────────────────────────────────────────────────────────────────────

    data class IPv4Result(
        val networkId: String,
        val broadcast: String,
        val subnetMask: String,
        val wildcardMask: String,
        val usableHosts: String,
        val firstHost: String,
        val lastHost: String,
        val cidr: String
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @param ipString   dotted-decimal IP, e.g. "192.168.1.10"
     * @param maskString dotted-decimal mask, e.g. "255.255.255.0"
     * @return IPv4Result, or null if parsing fails
     */
    fun calculate(ipString: String, maskString: String): IPv4Result? {
        val ipLong   = parseDottedDecimal(ipString)  ?: return null
        val maskLong = parseDottedDecimal(maskString) ?: return null

        // Validate that the mask is a valid contiguous subnet mask
        if (!isValidSubnetMask(maskLong)) return null

        val networkLong   = ipLong and maskLong
        val broadcastLong = networkLong or (maskLong.inv() and 0xFFFFFFFFL)
        val wildcardLong  = maskLong.inv() and 0xFFFFFFFFL

        val cidrPrefix    = maskToCidr(maskLong)

        // For /31 and /32, special-case host counts (RFC 3021 / loopback)
        val hostCount: Long = when (cidrPrefix) {
            32   -> 1L
            31   -> 2L
            else -> broadcastLong - networkLong - 1L
        }

        val firstHost: Long = when (cidrPrefix) {
            32   -> networkLong
            31   -> networkLong
            else -> networkLong + 1L
        }

        val lastHost: Long = when (cidrPrefix) {
            32   -> networkLong
            31   -> broadcastLong
            else -> broadcastLong - 1L
        }

        val formatter = NumberFormat.getNumberInstance(Locale.US)

        return IPv4Result(
            networkId   = longToDottedDecimal(networkLong),
            broadcast   = if (cidrPrefix == 32) longToDottedDecimal(networkLong)
                          else longToDottedDecimal(broadcastLong),
            subnetMask  = longToDottedDecimal(maskLong),
            wildcardMask = longToDottedDecimal(wildcardLong),
            usableHosts = when (cidrPrefix) {
                32   -> "1 (host route)"
                31   -> "2 (point-to-point)"
                else -> formatter.format(hostCount)
            },
            firstHost   = longToDottedDecimal(firstHost),
            lastHost    = longToDottedDecimal(lastHost),
            cidr        = "/$cidrPrefix  (${longToDottedDecimal(maskLong)})"
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parsing helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts "192.168.1.10" → Long (big-endian, unsigned 32-bit).
     * Returns null if format is wrong.
     */
    fun parseDottedDecimal(dotted: String): Long? {
        val parts = dotted.split(".")
        if (parts.size != 4) return null
        var result = 0L
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet < 0 || octet > 255) return null
            result = (result shl 8) or octet.toLong()
        }
        return result
    }

    /**
     * Converts a 32-bit Long back to dotted-decimal string.
     */
    fun longToDottedDecimal(value: Long): String {
        val b1 = (value shr 24) and 0xFF
        val b2 = (value shr 16) and 0xFF
        val b3 = (value shr 8)  and 0xFF
        val b4 =  value         and 0xFF
        return "$b1.$b2.$b3.$b4"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subnet mask helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Counts leading 1-bits in a 32-bit mask to derive CIDR prefix length.
     */
    fun maskToCidr(mask: Long): Int {
        var count = 0
        var m = mask
        for (i in 0 until 32) {
            if ((m and 0x80000000L) != 0L) count++ else break
            m = m shl 1
        }
        return count
    }

    /**
     * Converts CIDR prefix length (0-32) to a 32-bit mask Long.
     */
    fun cidrToMask(prefix: Int): Long {
        if (prefix == 0) return 0L
        if (prefix == 32) return 0xFFFFFFFFL
        return (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
    }

    /**
     * A valid subnet mask has all 1-bits contiguous from the MSB,
     * e.g. 11111111.11111111.11111111.00000000.
     * Masks like 255.0.255.0 are rejected.
     */
    fun isValidSubnetMask(mask: Long): Boolean {
        // Invert the mask: valid mask → all-1s prefix, so inverted is all-0s prefix + trailing 1s
        val inv = mask.inv() and 0xFFFFFFFFL
        // (inv + 1) should be a power of two, or inv is 0 (mask = /32) or inv = 0xFFFFFFFF (mask = /0)
        return inv == 0L || ((inv + 1L) and inv) == 0L
    }
}
