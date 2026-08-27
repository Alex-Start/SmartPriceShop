package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiScannedProductDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiPriceScannerService {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val dtoAdapter = moshi.adapter(AiScannedProductDto::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun scanPriceTagImage(bitmap: Bitmap, apiKeyOverride: String? = null): Result<AiScannedProductDto> = withContext(Dispatchers.IO) {
        val apiKey = (apiKeyOverride?.trim()?.takeIf { it.isNotEmpty() }
            ?: BuildConfig.GEMINI_API_KEY.trim()).takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }

        if (apiKey.isNullOrBlank()) {
            Log.w("GeminiScanner", "GEMINI_API_KEY is not configured.")
            val sampleParsed = AiScannedProductDto(
                productName = "Fresh Organic Butter",
                category = "Dairy",
                shopName = "Supermarket",
                regularPrice = 3.49,
                discountPrice = 2.89,
                packageAmount = 250.0,
                packageUnit = "g",
                notes = "AI key not configured. Set it in app settings to enable live AI scan."
            )
            return@withContext Result.success(sampleParsed)
        }

        try {
            val base64Image = bitmapToBase64(bitmap)

            val prompt = """
                Analyze this supermarket price tag, shelf label, receipt, or product packaging image carefully.
                Extract the following structured details in pure JSON format:
                {
                  "productName": "Exact or clean product name",
                  "category": "One of: Dairy, Fruits & Veg, Meat & Fish, Bakery, Beverages, Pantry, Snacks, Household, Other",
                  "shopName": "Supermarket or store name if visible, e.g. Tesco, Lidl, Aldi, Walmart, or null",
                  "shopAddress": "Store branch or location if visible, or null",
                  "regularPrice": 3.99 (numeric current shelf price or standard price. Always extract the main price on the tag into regularPrice),
                  "discountPrice": 2.99 (numeric promotional/clubcard/sale price if a separate discounted price is also present, otherwise null),
                  "packageAmount": 500.0 (numeric amount e.g. 500 for 500g, 1.0 for 1 piece),
                  "packageUnit": "g, kg, ml, L, pcs, or item",
                  "barcode": "Barcode numbers if legible, or null",
                  "notes": "Any deal notes e.g. 2 for 1, Clubcard price, 20% off"
                }
                Return ONLY valid JSON without markdown wrapping.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            // Text part
                            put(JSONObject().apply { put("text", prompt) })
                            // Image part
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API Error: ${response.code} $responseBody"))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text") ?: ""

            val cleanedJson = textPart.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsedDto = dtoAdapter.fromJson(cleanedJson)
                ?: return@withContext Result.failure(Exception("Could not parse AI response"))

            Result.success(parsedDto)
        } catch (e: Exception) {
            Log.e("GeminiScanner", "Error analyzing price tag", e)
            Result.failure(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        // Resize bitmap if very large to prevent memory overhead and speed up transfer
        val maxDim = 1024
        val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
        val scaledBitmap = if (ratio < 1.0f) {
            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
