package com.myfintrack.app.data

import com.myfintrack.app.domain.Money
import com.myfintrack.app.model.Transaction
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Writes transactions as UTF-8 CSV. Amounts are exported as plain rupee decimals derived
 * from paise (no floating point). Text cells are protected against spreadsheet formula
 * injection.
 */
object CsvExporter {

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /**
     * @param accountNames account id -> name, used for the account columns. Transfers export
     * the source in `account` and the destination in `to_account`.
     */
    fun write(output: OutputStream, transactions: List<Transaction>, accountNames: Map<Long, String>, zone: ZoneId) {
        val writer = output.bufferedWriter(Charsets.UTF_8)
        writer.write("date,type,title,category,payment_method,amount_inr,account,to_account,note\n")
        for (tx in transactions) {
            val date = DATE_FORMAT.format(Instant.ofEpochMilli(tx.occurredAt).atZone(zone))
            val row = listOf(
                date,
                tx.type.name,
                safeText(tx.title),
                safeText(tx.category.label),
                safeText(tx.paymentMethod.label),
                Money.toPlainRupees(tx.amountPaise),
                safeText(tx.accountId?.let { accountNames[it] } ?: ""),
                safeText(tx.toAccountId?.let { accountNames[it] } ?: ""),
                safeText(tx.note)
            ).joinToString(",") { escape(it) }
            writer.write(row)
            writer.write("\n")
        }
        writer.flush()
    }

    private fun safeText(value: String): String {
        val first = value.firstOrNull() ?: return value
        return if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t') "'$value" else value
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }
}
