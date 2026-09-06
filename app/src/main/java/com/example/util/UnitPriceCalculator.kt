package com.example.util

import java.util.Locale

object UnitPriceCalculator {

    /**
     * Converts weight / volume / count to total grams (or ml for liquids).
     * Returns null if the unit is purely item/piece count.
     */
    fun convertToGrams(amount: Double, unit: String): Double? {
        if (amount <= 0.0) return null
        val cleanUnit = unit.trim().lowercase()
        return when {
            cleanUnit == "g" || cleanUnit == "gram" || cleanUnit == "grams" || cleanUnit == "gr" -> amount
            cleanUnit == "kg" || cleanUnit == "kilo" || cleanUnit == "kilogram" || cleanUnit == "kgs" -> amount * 1000.0
            cleanUnit == "mg" || cleanUnit == "milligram" -> amount / 1000.0
            cleanUnit == "oz" || cleanUnit == "ounce" || cleanUnit == "ounces" -> amount * 28.3495
            cleanUnit == "lb" || cleanUnit == "lbs" || cleanUnit == "pound" || cleanUnit == "pounds" -> amount * 453.592
            cleanUnit == "ml" || cleanUnit == "milliliter" || cleanUnit == "millilitre" -> amount
            cleanUnit == "l" || cleanUnit == "liter" || cleanUnit == "litre" || cleanUnit == "litres" -> amount * 1000.0
            cleanUnit == "cl" -> amount * 10.0
            else -> null
        }
    }

    /**
     * Calculates the exact price per single gram.
     * Returns null if amount/unit cannot be converted to weight.
     */
    fun calculatePricePerGram(price: Double, amount: Double, unit: String): Double? {
        if (price <= 0.0 || amount <= 0.0) return null
        val grams = convertToGrams(amount, unit) ?: return null
        return if (grams > 0) price / grams else null
    }

    /**
     * Formats price per gram cleanly:
     * e.g. "$0.45/100g", "0.45 €/100g", "15.50 ₴/100g", "12.00 Kč/100g"
     */
    fun formatPricePerGram(
        price: Double,
        amount: Double,
        unit: String,
        includeDetailedGram: Boolean = false,
        currency: AppCurrency = AppCurrency.CZK
    ): String? {
        val pricePerG = calculatePricePerGram(price, amount, unit) ?: return null
        val pricePer100g = pricePerG * 100.0
        val cleanUnit = unit.trim().lowercase()
        val isLiquid = cleanUnit.contains("l") || cleanUnit.contains("ml")
        val unitMeasure = if (isLiquid) "100ml" else "100g"
        val singleMeasure = if (isLiquid) "ml" else "g"

        val formatted100 = currency.format(pricePer100g)
        val formattedSingle = if (currency.isSuffix) {
            String.format(Locale.US, "%.4f %s", pricePerG, currency.symbol)
        } else {
            String.format(Locale.US, "%s%.4f", currency.symbol, pricePerG)
        }

        return if (includeDetailedGram) {
            "$formatted100/$unitMeasure ($formattedSingle/$singleMeasure)"
        } else {
            "$formatted100/$unitMeasure"
        }
    }

    /**
     * Calculates the standardized unit price based on price, amount, and unit.
     */
    fun calculateUnitPrice(
        price: Double,
        amount: Double,
        unit: String,
        targetStandard: String = "per 100g",
        currency: AppCurrency = AppCurrency.CZK
    ): Pair<Double, String> {
        val sym = currency.symbol
        if (price <= 0.0 || amount <= 0.0) {
            return Pair(price, "$sym/item")
        }

        val cleanUnit = unit.trim().lowercase()

        return when {
            cleanUnit == "g" || cleanUnit == "gram" || cleanUnit == "grams" || cleanUnit == "gr" -> {
                val pricePer100g = (price / amount) * 100.0
                Pair(pricePer100g, "$sym/100g")
            }
            cleanUnit == "kg" || cleanUnit == "kilo" || cleanUnit == "kilogram" || cleanUnit == "kgs" -> {
                val pricePer100g = (price / (amount * 1000.0)) * 100.0
                Pair(pricePer100g, "$sym/100g")
            }
            cleanUnit == "oz" || cleanUnit == "ounce" -> {
                val grams = amount * 28.3495
                val pricePer100g = (price / grams) * 100.0
                Pair(pricePer100g, "$sym/100g")
            }
            cleanUnit == "lb" || cleanUnit == "lbs" || cleanUnit == "pound" -> {
                val grams = amount * 453.592
                val pricePer100g = (price / grams) * 100.0
                Pair(pricePer100g, "$sym/100g")
            }
            cleanUnit == "ml" || cleanUnit == "milliliter" -> {
                val pricePer100ml = (price / amount) * 100.0
                Pair(pricePer100ml, "$sym/100ml")
            }
            cleanUnit == "l" || cleanUnit == "liter" || cleanUnit == "litre" -> {
                val pricePer100ml = (price / (amount * 1000.0)) * 100.0
                Pair(pricePer100ml, "$sym/100ml")
            }
            cleanUnit == "pcs" || cleanUnit == "piece" || cleanUnit == "pieces" || cleanUnit == "item" || cleanUnit == "items" || cleanUnit == "unit" || cleanUnit == "pk" || cleanUnit == "pack" -> {
                val pricePerItem = price / amount
                Pair(pricePerItem, "$sym/item")
            }
            else -> {
                val pricePerItem = price / amount
                Pair(pricePerItem, "$sym/$unit")
            }
        }
    }

    fun formatWeight(amount: Double, unit: String): String {
        val cleanUnit = unit.trim().lowercase()
        return when {
            cleanUnit == "g" && amount >= 1000.0 -> String.format(Locale.US, "%.1fkg", amount / 1000.0)
            cleanUnit == "ml" && amount >= 1000.0 -> String.format(Locale.US, "%.1fL", amount / 1000.0)
            amount == amount.toInt().toDouble() -> "${amount.toInt()}$unit"
            else -> String.format(Locale.US, "%.1f%s", amount, unit)
        }
    }

    fun formatCurrency(amount: Double, currency: AppCurrency = AppCurrency.CZK): String {
        return currency.format(amount)
    }

    fun formatUnitPrice(unitPrice: Double, label: String, currency: AppCurrency = AppCurrency.CZK): String {
        val cleanLabel = label.replace(currency.symbol, "").replace("$", "").replace("€", "").replace("₴", "").replace("Kč", "").trim()
        val formattedPrice = currency.format(unitPrice)
        return "$formattedPrice $cleanLabel"
    }

    /**
     * Formats an amount stored in sourceCurrencyCode (default CZK) into the target AppCurrency,
     * performing conversion based on CurrencyRates.
     */
    fun formatCurrencyWithConversion(amount: Double, sourceCurrencyCode: String? = AppCurrency.CZK.code, targetCurrency: AppCurrency = AppCurrency.CZK): String {
        val src = sourceCurrencyCode ?: AppCurrency.CZK.code
        val converted = CurrencyRates.convert(amount, src, targetCurrency.code)
        return targetCurrency.format(converted)
    }
}
