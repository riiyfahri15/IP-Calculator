package com.ipcalc.app

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.ipcalc.app.databinding.ActivityMainBinding
import com.ipcalc.app.databinding.ItemResultRowBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private var isIPv4Mode = true

    companion object {
        private const val PREFS_NAME  = "ipcalc_prefs"
        private const val KEY_DARK    = "is_dark_mode"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme BEFORE super.onCreate / setContentView
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        applyThemeFromPrefs()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupThemeToggle()
        setupSegmentedButton()
        setupCalculateButton()
        applyMode(ipv4 = true)
        updateThemeToggleIcon()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Theme toggle
    // ─────────────────────────────────────────────────────────────────────────

    private fun applyThemeFromPrefs() {
        val isDark = prefs.getBoolean(KEY_DARK, false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else        AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupThemeToggle() {
        binding.btnThemeToggle.setOnClickListener {
            val currentlyDark = prefs.getBoolean(KEY_DARK, false)
            val newDark       = !currentlyDark

            // Persist preference
            prefs.edit().putBoolean(KEY_DARK, newDark).apply()

            // Rotate icon animation before recreating
            ObjectAnimator.ofFloat(binding.btnThemeToggle, View.ROTATION, 0f, 360f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }

            // Apply — recreates Activity automatically
            AppCompatDelegate.setDefaultNightMode(
                if (newDark) AppCompatDelegate.MODE_NIGHT_YES
                else         AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    /** Moon icon = currently in light mode (next click → dark).
     *  Sun icon  = currently in dark mode  (next click → light). */
    private fun updateThemeToggleIcon() {
        val isDark = prefs.getBoolean(KEY_DARK, false)
        binding.btnThemeToggle.setImageResource(
            if (isDark) R.drawable.ic_light_mode
            else        R.drawable.ic_dark_mode
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toolbar
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Segmented Button
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupSegmentedButton() {
        binding.btnIPv4.setOnClickListener {
            if (!isIPv4Mode) applyMode(ipv4 = true)
        }
        binding.btnIPv6.setOnClickListener {
            if (isIPv4Mode) applyMode(ipv4 = false)
        }
    }

    private fun applyMode(ipv4: Boolean) {
        isIPv4Mode = ipv4
        updateSegmentVisuals()
        updateInputVisibility()
        resetResultPanel()
        clearAllErrors()
    }

    private fun updateSegmentVisuals() {
        val selectedTextColor   = ContextCompat.getColor(this, R.color.segment_selected_text)
        val unselectedTextColor = ContextCompat.getColor(this, R.color.segment_unselected_text)

        if (isIPv4Mode) {
            binding.btnIPv4.setBackgroundResource(R.drawable.bg_segment_selected)
            binding.btnIPv4.setTextColor(selectedTextColor)
            binding.btnIPv4.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.btnIPv6.setBackgroundResource(R.drawable.bg_segment_unselected)
            binding.btnIPv6.setTextColor(unselectedTextColor)
            binding.btnIPv6.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            binding.btnIPv6.setBackgroundResource(R.drawable.bg_segment_selected)
            binding.btnIPv6.setTextColor(selectedTextColor)
            binding.btnIPv6.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.btnIPv4.setBackgroundResource(R.drawable.bg_segment_unselected)
            binding.btnIPv4.setTextColor(unselectedTextColor)
            binding.btnIPv4.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun updateInputVisibility() {
        if (isIPv4Mode) {
            binding.layoutIPv4Input.visibility = View.VISIBLE
            binding.layoutIPv6Input.visibility = View.GONE
            binding.etIpAddress.text?.clear()
            binding.etIpv4Prefix.text?.clear()
        } else {
            binding.layoutIPv4Input.visibility = View.GONE
            binding.layoutIPv6Input.visibility = View.VISIBLE
            binding.etIpv6Address.text?.clear()
            binding.etPrefixLength.text?.clear()
        }
    }

    private fun resetResultPanel() {
        binding.layoutIPv4Results.visibility = View.GONE
        binding.layoutIPv6Results.visibility = View.GONE
        binding.tvEmptyResult.visibility     = View.VISIBLE
        binding.dotIndicator.setBackgroundResource(R.drawable.bg_dot_inactive)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Calculate Button
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupCalculateButton() {
        binding.btnCalculate.setOnClickListener {
            hideKeyboard()
            animateButtonPress(binding.btnCalculate)
            if (isIPv4Mode) performIPv4Calculation()
            else            performIPv6Calculation()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IPv4 Flow
    // ─────────────────────────────────────────────────────────────────────────

    private fun performIPv4Calculation() {
        clearAllErrors()

        val ip        = binding.etIpAddress.text?.toString()?.trim() ?: ""
        val prefixStr = binding.etIpv4Prefix.text?.toString()?.trim() ?: ""

        val ipError = InputValidator.validateIPv4(ip)
        if (ipError != null) { binding.tilIpAddress.error = ipError; return }

        val prefixError = InputValidator.validateIPv4Prefix(prefixStr)
        if (prefixError != null) { binding.tilIpv4Prefix.error = prefixError; return }

        val maskStr = IPv4Calculator.longToDottedDecimal(
            IPv4Calculator.cidrToMask(prefixStr.toInt())
        )
        val result = IPv4Calculator.calculate(ip, maskStr) ?: run {
            binding.tilIpAddress.error = getString(R.string.error_invalid_ipv4); return
        }
        displayIPv4Results(result)
    }

    private fun displayIPv4Results(result: IPv4Calculator.IPv4Result) {
        binding.tvEmptyResult.visibility     = View.GONE
        binding.layoutIPv4Results.visibility = View.VISIBLE
        binding.layoutIPv6Results.visibility = View.GONE
        pulseDotIndicator()

        setResultRow(binding.rowNetworkId,   getString(R.string.result_network_id),   result.networkId)
        setResultRow(binding.rowBroadcast,   getString(R.string.result_broadcast),    result.broadcast)
        setResultRow(binding.rowSubnetMask,  getString(R.string.result_subnet_mask),  result.subnetMask)
        setResultRow(binding.rowWildcard,    getString(R.string.result_wildcard),     result.wildcardMask)
        setResultRow(binding.rowUsableHosts, getString(R.string.result_usable_hosts), result.usableHosts)
        setResultRow(binding.rowFirstHost,   getString(R.string.result_first_host),   result.firstHost)
        setResultRow(binding.rowLastHost,    getString(R.string.result_last_host),    result.lastHost)
        setResultRow(binding.rowCidr,        getString(R.string.result_cidr_prefix),  result.cidr)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IPv6 Flow
    // ─────────────────────────────────────────────────────────────────────────

    private fun performIPv6Calculation() {
        clearAllErrors()

        val address   = binding.etIpv6Address.text?.toString()?.trim() ?: ""
        val prefixStr = binding.etPrefixLength.text?.toString()?.trim() ?: ""

        val addressError = InputValidator.validateIPv6(address)
        if (addressError != null) { binding.tilIpv6Address.error = addressError; return }

        val prefixError = InputValidator.validateIPv6Prefix(prefixStr)
        if (prefixError != null) { binding.tilPrefixLength.error = prefixError; return }

        val result = IPv6Calculator.calculate(address, prefixStr.toInt()) ?: run {
            binding.tilIpv6Address.error = getString(R.string.error_invalid_ipv6); return
        }
        displayIPv6Results(result)
    }

    private fun displayIPv6Results(result: IPv6Calculator.IPv6Result) {
        binding.tvEmptyResult.visibility     = View.GONE
        binding.layoutIPv4Results.visibility = View.GONE
        binding.layoutIPv6Results.visibility = View.VISIBLE
        pulseDotIndicator()

        setResultRow(binding.rowExpanded,      getString(R.string.result_expanded),       result.expandedAddress)
        setResultRow(binding.rowNetworkPrefix, getString(R.string.result_network_prefix), result.networkPrefix)
        setResultRow(binding.rowAddressRange,  getString(R.string.result_address_range),  result.addressRange)
        setResultRow(binding.rowTotalIps,      getString(R.string.result_total_ips),      result.totalIPs)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Animations
    // ─────────────────────────────────────────────────────────────────────────

    private fun pulseDotIndicator() {
        binding.dotIndicator.setBackgroundResource(R.drawable.bg_dot_indicator)
        ObjectAnimator.ofPropertyValuesHolder(
            binding.dotIndicator,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.6f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.6f, 1f),
            PropertyValuesHolder.ofFloat(View.ALPHA,   1f, 0.6f, 1f)
        ).apply {
            duration     = 400
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun animateButtonPress(view: View) {
        ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.96f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.96f, 1f)
        ).apply {
            duration     = 150
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun setResultRow(rowBinding: ItemResultRowBinding, label: String, value: String) {
        rowBinding.tvLabel.text = label
        rowBinding.tvValue.text = value
    }

    private fun clearAllErrors() {
        binding.tilIpAddress.error    = null
        binding.tilIpv4Prefix.error   = null
        binding.tilIpv6Address.error  = null
        binding.tilPrefixLength.error = null
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: binding.root
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
