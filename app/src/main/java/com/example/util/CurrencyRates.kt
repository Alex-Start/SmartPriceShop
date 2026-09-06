package com.example.util

import android.content.Context
import org.json.JSONObject

/**
 * Simple in-memory currency rates store.
 * Rates are expressed as: how many units of TARGET currency equals 1 USD.
 * Example: rates["CZK"] = 22.0 means 1 USD = 22.0 CZK
 */
object CurrencyRates {
    private val rates: MutableMap<String, Double> = mutableMapOf(
        AppCurrency.USD.code to 1.0,
        AppCurrency.CZK.code to 22.0,
        AppCurrency.EUR.code to 0.92,
        AppCurrency.UAH.code to 36.0
    )

    fun getRates(): Map<String, Double> = rates.toMap()

    fun setRate(code: String, ratePer1USD: Double) {
        rates[code.uppercase()] = ratePer1USD
    }

    fun getRate(code: String): Double? = rates[code.uppercase()]

    /**
     * Converts amount from sourceCurrency to targetCurrency using USD as pivot:
     * converted = amount * (rate[target] / rate[source])
     * Both rates are expressed as units per 1 USD.
     */
    fun convert(amount: Double, sourceCurrency: String?, targetCurrency: String): Double {
        val src = (sourceCurrency ?: AppCurrency.CZK.code).uppercase()
        val tgt = targetCurrency.uppercase()
        val srcRate = rates[src] ?: 1.0
        val tgtRate = rates[tgt] ?: 1.0
        if (srcRate <= 0.0) return amount
        return amount * (tgtRate / srcRate)
    }

    fun loadFromPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences("price_tracker_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("currency_rates_json", null) ?: return
            val obj = JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optDouble(k, Double.NaN)
                if (!v.isNaN()) rates[k.uppercase()] = v
            }
        } catch (_: Exception) {
        }
    }

    fun saveToPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences("price_tracker_prefs", Context.MODE_PRIVATE)
            val obj = JSONObject()
            for ((k, v) in rates) obj.put(k, v)
            prefs.edit().putString("currency_rates_json", obj.toString()).apply()
        } catch (_: Exception) {
        }
    }
}
