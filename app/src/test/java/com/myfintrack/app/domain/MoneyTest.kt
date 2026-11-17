package com.myfintrack.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    private fun valid(input: String): Long {
        val result = Money.parseToPaise(input)
        assertTrue("Expected valid for '$input' but was $result", result is Money.ParseResult.Valid)
        return (result as Money.ParseResult.Valid).paise
    }

    private fun invalid(input: String) {
        assertTrue("Expected invalid for '$input'", Money.parseToPaise(input) is Money.ParseResult.Invalid)
    }

    @Test
    fun parsesWholeAndDecimalRupeesIntoPaise() {
        assertEquals(10_000L, valid("100"))
        assertEquals(12_050L, valid("120.5"))
        assertEquals(12_055L, valid("120.55"))
        assertEquals(1L, valid("0.01"))
        assertEquals(123_456_700L, valid("12,34,567"))
        assertEquals(10_000L, valid("₹100"))
        assertEquals(10_000L, valid("100.00"))
    }

    @Test
    fun rejectsBadAmounts() {
        invalid("")
        invalid("0")
        invalid("0.00")
        invalid("-5")
        invalid("abc")
        invalid("1.234")
        invalid("1e5")
        invalid("1000000000") // above ₹99,99,99,999.99
    }

    @Test
    fun avoidsFloatingPointErrors() {
        // 0.1 + 0.2 must be exactly 30 paise
        assertEquals(30L, valid("0.1") + valid("0.2"))
    }

    @Test
    fun formatsWithIndianGrouping() {
        assertEquals("₹0.00", Money.format(0L))
        assertEquals("₹999.00", Money.format(99_900L))
        assertEquals("₹1,000.00", Money.format(100_000L))
        assertEquals("₹12,34,567.89", Money.format(123_456_789L))
        assertEquals("₹1,00,00,000.00", Money.format(1_000_000_000L))
        assertEquals("-₹1,500.50", Money.format(-150_050L))
        assertEquals("₹1,501", Money.format(150_050L, withDecimals = false))
    }

    @Test
    fun formatsCompact() {
        assertEquals("₹950", Money.formatCompact(95_000L))
        assertEquals("₹12.5K", Money.formatCompact(1_250_000L))
        assertEquals("₹3.4L", Money.formatCompact(34_000_000L))
        assertEquals("₹1.25Cr", Money.formatCompact(1_250_000_000L))
    }

    @Test
    fun editableStringRoundTrips() {
        assertEquals("120.5", Money.toEditableString(12_050L))
        assertEquals("100", Money.toEditableString(10_000L))
        assertEquals("120.50", Money.toPlainRupees(12_050L))
    }
}
