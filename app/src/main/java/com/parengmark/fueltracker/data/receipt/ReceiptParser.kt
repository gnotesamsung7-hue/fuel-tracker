package com.parengmark.fueltracker.data.receipt

/** Best-effort values pulled from a receipt's recognized text. Any field can be
 * null -- the caller always shows these as an editable, pre-filled form for the
 * user to confirm rather than saving them directly (see the spec's OCR section:
 * receipts vary a lot in layout and on-device OCR makes mistakes). */
data class ParsedReceipt(
    val totalCost: Double? = null,
    val volume: Double? = null,
    val pricePerUnit: Double? = null,
    val stationGuess: String? = null,
    val rawText: String = ""
)

/**
 * Rule-based parsing over ML Kit's recognized text -- no cloud call, no paid
 * API. Looks for currency-like and volume-like numbers near recognizable
 * keywords, then cross-checks the three fields against each other
 * (total = volume x price/unit) to fill in whichever one wasn't found
 * directly, when the other two are confident matches.
 */
object ReceiptParser {

    private val totalKeywordRegex = Regex("""(?i)\btotal\b(?!.*\bsub)""")
    private val volumeKeywordRegex = Regex("""(?i)\b(liters?|litres?|volume|gallons?|gal|qty)\b""")
    private val priceKeywordRegex = Regex("""(?i)\b(price|rate|unit\s*price|p/l|/l|/gal)\b""")

    private val amountRegex = Regex("""(\d{1,3}(?:[,\d]{0,10})?\.\d{2})""")
    private val volumeAmountRegex = Regex("""(\d{1,4}\.\d{2,3})\s*(L|LTR|LITERS?|LITRES?|GAL|GALLONS?)?""", RegexOption.IGNORE_CASE)
    private val smallPriceRegex = Regex("""(\d{1,3}\.\d{2,3})""")

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var total: Double? = null
        var volume: Double? = null
        var pricePerUnit: Double? = null

        for (line in lines) {
            if (total == null && totalKeywordRegex.containsMatchIn(line)) {
                amountRegex.find(line.replace(",", ""))?.let {
                    total = it.value.toDoubleOrNull()
                }
            }
            if (volume == null && volumeKeywordRegex.containsMatchIn(line)) {
                volumeAmountRegex.find(line)?.let {
                    volume = it.groupValues[1].toDoubleOrNull()
                }
            }
            if (pricePerUnit == null && priceKeywordRegex.containsMatchIn(line)) {
                smallPriceRegex.find(line)?.let {
                    pricePerUnit = it.value.toDoubleOrNull()
                }
            }
        }

        // Fallback pass: if a keyword-anchored search found nothing, take the
        // single largest decimal-with-2-places number on the receipt as the
        // total (most receipts print the grand total as their biggest amount).
        if (total == null) {
            total = lines
                .flatMap { amountRegex.findAll(it.replace(",", "")).map { m -> m.value } }
                .mapNotNull { it.toDoubleOrNull() }
                .maxOrNull()
        }

        // Cross-check / fill in the third value from the other two when we're
        // missing exactly one of them.
        if (total != null && volume != null && pricePerUnit == null && volume!! > 0) {
            pricePerUnit = total!! / volume!!
        } else if (total != null && pricePerUnit != null && volume == null && pricePerUnit!! > 0) {
            volume = total!! / pricePerUnit!!
        } else if (volume != null && pricePerUnit != null && total == null) {
            total = volume!! * pricePerUnit!!
        }

        val stationGuess = lines.firstOrNull { it.length in 3..40 && !it.any { c -> c.isDigit() } }

        return ParsedReceipt(
            totalCost = total?.let { Math.round(it * 100) / 100.0 },
            volume = volume?.let { Math.round(it * 1000) / 1000.0 },
            pricePerUnit = pricePerUnit,
            stationGuess = stationGuess,
            rawText = rawText
        )
    }
}
