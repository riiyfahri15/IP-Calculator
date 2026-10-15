package com.ipcalc.app

/**
 * Stateless input validation for IPv4 and IPv6 fields.
 * Every function returns a human-readable error string on failure, or null on success.
 */
object InputValidator {

    // ─────────────────────────────────────────────────────────────────────────
    // IPv4 Validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates a dotted-decimal IPv4 address string entered by the user.
     *
     * Rules:
     *  - Must not be blank.
     *  - Must contain exactly 3 dots (4 octets).
     *  - Each octet must be a valid integer in 0–255.
     *  - Leading zeros are rejected (e.g. "01" is invalid — avoids octal confusion).
     */
    fun validateIPv4(ip: String): String? {
        if (ip.isBlank()) return "Please enter an IP address"

        val parts = ip.split(".")
        if (parts.size != 4) return "Invalid IPv4 format — must have 4 octets (e.g. 192.168.1.1)"

        for ((index, part) in parts.withIndex()) {
            if (part.isEmpty()) return "Octet ${index + 1} is empty"

            // Reject leading zeros (except "0" itself)
            if (part.length > 1 && part.startsWith("0")) {
                return "Octet ${index + 1}: leading zeros are not allowed"
            }

            val value = part.toIntOrNull()
                ?: return "Octet ${index + 1} is not a number"

            if (value < 0 || value > 255) {
                return "Octet ${index + 1} must be 0–255 (got $value)"
            }
        }
        return null
    }

    /**
     * Validates the four individual subnet mask octet strings.
     * Called with the text content of each of the four mask EditTexts.
     *
     * Rules:
     *  - None may be blank.
     *  - Each must be 0–255.
     *  - The assembled mask must be a valid contiguous subnet mask
     *    (all 1-bits must precede all 0-bits in the 32-bit representation).
     */
    fun validateIPv4Mask(o1: String, o2: String, o3: String, o4: String): String? {
        val octets = listOf(o1, o2, o3, o4)

        for ((index, octet) in octets.withIndex()) {
            if (octet.isBlank()) return "Mask octet ${index + 1} is empty"

            val value = octet.toIntOrNull()
                ?: return "Mask octet ${index + 1} is not a number"

            if (value < 0 || value > 255) {
                return "Mask octet ${index + 1} must be 0–255 (got $value)"
            }
        }

        val maskStr = "${o1}.${o2}.${o3}.${o4}"
        val maskLong = IPv4Calculator.parseDottedDecimal(maskStr)
            ?: return "Invalid subnet mask format"

        if (!IPv4Calculator.isValidSubnetMask(maskLong)) {
            return "Invalid subnet mask — bits must be contiguous (e.g. 255.255.255.0)"
        }

        return null
    }

    /**
     * Validates a CIDR prefix length for IPv4 (0–32).
     * Not currently used in the main UI (mask is entered as dotted-decimal),
     * but provided for completeness and future use.
     */
    fun validateIPv4Prefix(prefixStr: String): String? {
        if (prefixStr.isBlank()) return "Please enter a prefix length"
        val value = prefixStr.toIntOrNull()
            ?: return "Prefix length must be a number"
        if (value < 0 || value > 32) return "IPv4 prefix must be between 0 and 32 (got $value)"
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IPv6 Validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates an IPv6 address string.
     *
     * Supports:
     *  - Full 8-group form:  "2001:0db8:0000:0000:0000:0000:0000:0001"
     *  - Compressed form:    "2001:db8::1", "::1", "::", "fe80::1"
     *  - Zone IDs stripped:  "fe80::1%eth0" (zone part is ignored)
     *
     * Rules enforced:
     *  - Must not be blank.
     *  - Must not contain more than one "::" sequence.
     *  - Each group must be 1–4 hex characters.
     *  - Total groups (after "::" expansion) must equal exactly 8.
     *  - No embedded IPv4 (e.g. "::ffff:192.168.1.1") — not supported in this app.
     */
    fun validateIPv6(address: String): String? {
        if (address.isBlank()) return "Please enter an IPv6 address"

        // Strip zone ID
        val addr = address.substringBefore('%').trim().lowercase()

        if (addr.isEmpty()) return "IPv6 address cannot be empty"

        // Check for invalid characters (only hex digits and colons allowed)
        val validChars = Regex("^[0-9a-f:]+$")
        if (!validChars.matches(addr)) {
            return "IPv6 address contains invalid characters (only 0-9, a-f, A-F, : allowed)"
        }

        // Only one "::" is allowed
        if (addr.contains(":::")) {
            return "Invalid IPv6 address — ':::' is not allowed"
        }
        val doubleColonCount = countOccurrences(addr, "::")
        if (doubleColonCount > 1) {
            return "Invalid IPv6 address — only one '::' is allowed"
        }

        // Attempt full expansion to verify group count
        val expanded = IPv6Calculator.expandIPv6(addr)
            ?: return "Invalid IPv6 address format"

        val groups = expanded.split(":")
        if (groups.size != 8) {
            return "Invalid IPv6 address — must expand to exactly 8 groups"
        }

        // Validate each group is valid hex (1–4 chars)
        for ((i, group) in groups.withIndex()) {
            if (group.isEmpty() || group.length > 4) {
                return "Invalid IPv6 group ${i + 1}: '$group'"
            }
            if (!group.all { it.isDigit() || it in 'a'..'f' }) {
                return "Invalid hex in IPv6 group ${i + 1}: '$group'"
            }
        }

        return null
    }

    /**
     * Validates a CIDR prefix length for IPv6 (0–128).
     *
     * Rules:
     *  - Must not be blank.
     *  - Must be a valid integer.
     *  - Must be in range 0–128.
     */
    fun validateIPv6Prefix(prefixStr: String): String? {
        if (prefixStr.isBlank()) return "Please enter a prefix length"

        val value = prefixStr.toIntOrNull()
            ?: return "Prefix length must be a number"

        if (value < 0 || value > 128) {
            return "IPv6 prefix must be between 0 and 128 (got $value)"
        }

        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Counts non-overlapping occurrences of [sub] within [str].
     */
    private fun countOccurrences(str: String, sub: String): Int {
        var count = 0
        var idx   = 0
        while (true) {
            idx = str.indexOf(sub, idx)
            if (idx == -1) break
            count++
            idx += sub.length
        }
        return count
    }
}
