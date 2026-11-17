package com.myfintrack.app.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money helpers. All amounts are integer paise (Long). Parsing uses BigDecimal so no
 * floating point value ever touches a stored amount.
 */
object Money {

    /** Upper bound for a single amount: ₹99,99,99,999.99 */
    const val MAX_PAISE: Long = 99_999_999_999L

    const val MASK = "₹ ••••"

    private val MAX_RUPEES: BigDecimal = BigDecimal.valueOf(MAX_PAISE, 2)

    sealed class ParseResult {
        data class Valid(val paise: Long) : ParseResult()
        data class Invalid(val reason: String) : ParseResult()
    }

    /** Characters the amount field accepts while typing: up to 9 integer digits and 2 decimals. */
    private val TYPING_PATTERN = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

    fun isAcceptableWhileTyping(input: String): Boolean = TYPING_PATTERN.matches(input)

    fun parseToPaise(input: String): ParseResult {
        val cleaned = input.trim().removePrefix("₹").replace(",", "").trim()
        if (cleaned.isEmpty()) return ParseResult.Invalid("Enter an amount")
        if (cleaned.any { it == 'e' || it == 'E' || it == '+' || it == '-' }) {
            return ParseResult.Invalid("Enter a valid amount")
        }
        val value = try {
            BigDecimal(cleaned)
        } catch (e: NumberFormatException) {
            return ParseResult.Invalid("Enter a valid amount")
        }
        if (value.signum() <= 0) return ParseResult.Invalid("Amount must be greater than zero")
        if (value.stripTrailingZeros().scale() > 2) {
            return ParseResult.Invalid("Use at most 2 decimal places")
        }
        if (value > MAX_RUPEES) return ParseResult.Invalid("Amount is too large")
        val paise = value.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
        return ParseResult.Valid(paise)
    }

    /** Converts paise into a plain editable string, e.g. 12050 -> "120.5", 10000 -> "100". */
    fun toEditableString(paise: Long): String =
        BigDecimal.valueOf(paise, 2).stripTrailingZeros().toPlainString()

    /** Plain decimal rupee string with exactly 2 decimals, e.g. for CSV: 12050 -> "120.50". */
    fun toPlainRupees(paise: Long): String = BigDecimal.valueOf(paise, 2).toPlainString()

    /**
     * Indian-grouped currency string.
     * 123456789 paise -> "₹12,34,567.89". With [withDecimals] = false, rounds half-up to rupees.
     */
    fun format(paise: Long, withDecimals: Boolean = true): String {
        val negative = paise < 0
        val abs = if (negative) -paise else paise
        val body = if (withDecimals) {
            val rupees = abs / 100
            val fraction = abs % 100
            groupIndian(rupees) + "." + fraction.toString().padStart(2, '0')
        } else {
            groupIndian((abs + 50) / 100)
        }
        return (if (negative) "-₹" else "₹") + body
    }

    /** Like [format] but always prefixed with + or − (true minus sign). */
    fun formatSigned(paise: Long, positive: Boolean, withDecimals: Boolean = true): String {
        val abs = if (paise < 0) -paise else paise
        return (if (positive) "+" else "−") + format(abs, withDecimals)
    }

    /** Compact Indian notation for tight spaces: ₹950, ₹12.5K, ₹3.4L, ₹1.25Cr. */
    fun formatCompact(paise: Long): String {
        val negative = paise < 0
        val abs = if (negative) -paise else paise
        val rupees = (abs + 50) / 100
        val sign = if (negative) "-" else ""
        return when {
            rupees >= 10_000_000L -> sign + "₹" + scaled(rupees, 10_000_000L) + "Cr"
            rupees >= 100_000L -> sign + "₹" + scaled(rupees, 100_000L) + "L"
            rupees >= 1_000L -> sign + "₹" + scaled(rupees, 1_000L) + "K"
            else -> sign + "₹" + rupees.toString()
        }
    }

    /** Formats or masks depending on the privacy setting. */
    fun display(paise: Long, hidden: Boolean, withDecimals: Boolean = true): String =
        if (hidden) MASK else format(paise, withDecimals)

    private fun scaled(rupees: Long, unit: Long): String =
        BigDecimal.valueOf(rupees)
            .divide(BigDecimal.valueOf(unit), 2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    internal fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val lastThree = digits.substring(digits.length - 3)
        val rest = digits.substring(0, digits.length - 3)
        val parts = ArrayList<String>()
        var end = rest.length
        while (end > 0) {
            val start = if (end - 2 > 0) end - 2 else 0
            parts.add(0, rest.substring(start, end))
            end = start
        }
        return parts.joinToString(",") + "," + lastThree
    }
}
