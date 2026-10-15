package com.ipcalc.app

import java.math.BigInteger

/**
 * Pure calculation engine for IPv6 subnet math.
 *
 * Strategy:
 *  - Expand any compressed address (e.g. "2001:db8::1") to full 8-group hex.
 *  - Convert the 128-bit address to a BigInteger for bitwise arithmetic.
 *  - Apply the prefix mask to find the network prefix address.
 *  - Derive the last address in the block.
 *  - Compute 2^(128-prefix) for total IPs, formatted in both decimal and
 *    scientific notation when the number is very large.
 */
object IPv6Calculator {

    // ─────────────────────────────────────────────────────────────────────────
    // Result data class
    // ─────────────────────────────────────────────────────────────────────────

    data class IPv6Result(
        /** Full 8-group expanded address, e.g. "2001:0db8:0000:0000:0000:0000:0000:0001" */
        val expandedAddress: String,
        /** Network prefix address with CIDR notation, e.g. "2001:0db8::/32" */
        val networkPrefix: String,
        /** First and last address of the block joined with " –\n", hex format */
        val addressRange: String,
        /** 2^(128-prefix), formatted with commas and scientific notation */
        val totalIPs: String
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @param addressInput raw user input, may be compressed (e.g. "::1", "2001:db8::1")
     * @param prefixLength 0-128
     * @return IPv6Result, or null if parsing fails
     */
    fun calculate(addressInput: String, prefixLength: Int): IPv6Result? {
        // Step 1 – expand to 8 full groups
        val expanded = expandIPv6(addressInput) ?: return null

        // Step 2 – convert to BigInteger (128-bit unsigned)
        val addressBig = hexGroupsToBigInteger(expanded) ?: return null

        // Step 3 – compute network mask  (prefixLength leading 1-bits)
        val mask = buildMask(prefixLength)

        // Step 4 – network address = address AND mask
        val networkBig = addressBig.and(mask)

        // Step 5 – last address = network OR (NOT mask) trimmed to 128 bits
        val hostMask   = mask.not().and(MAX_128)
        val lastBig    = networkBig.or(hostMask)

        // Step 6 – total IPs = 2^(128-prefix)
        val hostBits   = 128 - prefixLength
        val totalBig   = BigInteger.valueOf(2L).pow(hostBits)

        return IPv6Result(
            expandedAddress = formatExpanded(expanded),
            networkPrefix   = bigIntegerToIPv6(networkBig) + "/$prefixLength",
            addressRange    = buildRangeString(networkBig, lastBig),
            totalIPs        = formatTotalIPs(totalBig)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IPv6 Expansion  (handles "::" compression)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Expands a compressed IPv6 address into exactly 8 colon-separated
     * 4-digit hex groups.
     *
     * Handles:
     *   "2001:db8::1"        → "2001:0db8:0000:0000:0000:0000:0000:0001"
     *   "::1"                → "0000:0000:0000:0000:0000:0000:0000:0001"
     *   "::"                 → "0000:0000:0000:0000:0000:0000:0000:0000"
     *   "fe80::1%eth0"       → strip zone ID first
     *   full 8-group address → pad each group to 4 hex digits
     */
    fun expandIPv6(input: String): String? {
        // Strip zone ID (e.g. "%eth0")
        val addr = input.substringBefore('%').trim().lowercase()

        if (addr.isEmpty()) return null

        return if (addr.contains("::")) {
            expandCompressed(addr)
        } else {
            expandFull(addr)
        }
    }

    private fun expandCompressed(addr: String): String? {
        // Split at "::" — at most two halves
        val sides = addr.split("::", limit = 2)
        if (sides.size != 2) return null

        val left  = if (sides[0].isEmpty()) emptyList() else sides[0].split(":")
        val right = if (sides[1].isEmpty()) emptyList() else sides[1].split(":")

        val totalExplicit = left.size + right.size
        if (totalExplicit > 8) return null

        val missingGroups = 8 - totalExplicit
        val middle = List(missingGroups) { "0000" }

        val allGroups = left + middle + right
        if (allGroups.size != 8) return null

        return allGroups.joinToString(":") { group ->
            group.padStart(4, '0')
        }
    }

    private fun expandFull(addr: String): String? {
        val groups = addr.split(":")
        if (groups.size != 8) return null
        val padded = mutableListOf<String>()
        for (group in groups) {
            if (group.length > 4) return null
            padded.add(group.padStart(4, '0'))
        }
        return padded.joinToString(":")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Conversion helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts the expanded 8-group string to a 128-bit BigInteger.
     * e.g. "2001:0db8:0000:..." → BigInteger
     */
    private fun hexGroupsToBigInteger(expanded: String): BigInteger? {
        val hex = expanded.replace(":", "")
        if (hex.length != 32) return null
        return try {
            BigInteger(hex, 16)
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Converts a 128-bit BigInteger back to a full expanded IPv6 string.
     * e.g. BigInteger → "2001:0db8:0000:0000:0000:0000:0000:0001"
     */
    private fun bigIntegerToIPv6(value: BigInteger): String {
        // Pad hex to exactly 32 characters
        val hex = value.toString(16).padStart(32, '0')
        return (0 until 8).joinToString(":") { i ->
            hex.substring(i * 4, i * 4 + 4)
        }
    }

    /**
     * Builds a 128-bit mask with [prefixLength] leading 1-bits.
     */
    private fun buildMask(prefixLength: Int): BigInteger {
        if (prefixLength == 0)   return BigInteger.ZERO
        if (prefixLength == 128) return MAX_128
        // Shift 1 left by (128 - prefix), subtract 1 to get host mask, XOR with all-1s
        val hostMask = BigInteger.ONE.shiftLeft(128 - prefixLength).subtract(BigInteger.ONE)
        return MAX_128.xor(hostMask)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formatting
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Formats the expanded address with uppercase hex groups separated by colons.
     * e.g. "2001:0DB8:0000:0000:0000:0000:0000:0001"
     */
    private fun formatExpanded(expanded: String): String {
        return expanded.uppercase()
    }

    /**
     * Builds a human-readable range string:
     *   "2001:0DB8:0000:... –
     *    2001:0DB8:FFFF:..."
     */
    private fun buildRangeString(first: BigInteger, last: BigInteger): String {
        val firstStr = bigIntegerToIPv6(first).uppercase()
        val lastStr  = bigIntegerToIPv6(last).uppercase()
        return "$firstStr\n–\n$lastStr"
    }

    /**
     * Formats total IP count.
     * - For small values (≤ 10^15): plain decimal with comma separators.
     * - For large values: "X.XXe+NN" scientific notation + exact decimal.
     *
     * Examples:
     *   256        → "256"
     *   65,536     → "65,536"
     *   2^64       → "1.845e+19\n(18,446,744,073,709,551,616)"
     *   2^128      → "3.403e+38\n(340,282,366,920,938,463,463,374,607,431,768,211,456)"
     */
    private fun formatTotalIPs(total: BigInteger): String {
        val threshold = BigInteger.TEN.pow(15)
        return if (total <= threshold) {
            formatWithCommas(total)
        } else {
            val sci   = toScientificNotation(total)
            val exact = formatWithCommas(total)
            "$sci\n($exact)"
        }
    }

    /**
     * Inserts commas every 3 digits into a BigInteger string.
     * e.g. "18446744073709551616" → "18,446,744,073,709,551,616"
     */
    private fun formatWithCommas(value: BigInteger): String {
        val str    = value.toString()
        val sb     = StringBuilder()
        val offset = str.length % 3  // how many digits before the first comma group
        str.forEachIndexed { i, c ->
            // Insert comma when we cross into a new group of 3, but never before the first char
            if (i > 0 && i == offset) {
                sb.append(',')
            } else if (i > offset && offset > 0 && (i - offset) % 3 == 0) {
                sb.append(',')
            } else if (offset == 0 && i > 0 && i % 3 == 0) {
                sb.append(',')
            }
            sb.append(c)
        }
        return sb.toString()
    }

    /**
     * Converts a BigInteger to scientific notation with 3 decimal places.
     * e.g. 2^64 = 18446744073709551616 → "1.845e+19"
     */
    private fun toScientificNotation(value: BigInteger): String {
        val str      = value.toString()
        val exponent = str.length - 1
        val mantissa = if (str.length >= 4) {
            "${str[0]}.${str.substring(1, 4)}"
        } else {
            str[0].toString()
        }
        return "${mantissa}e+$exponent"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────

    /** 2^128 - 1 : all 128 bits set */
    private val MAX_128: BigInteger =
        BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE)
}
