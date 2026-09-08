package com.visionassist.app.product

import android.util.Log
import com.visionassist.app.settings.AppSettings
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ProductRepository(private val appSettings: AppSettings) {

    companion object {
        private const val TAG = "ProductRepository"
        private const val BASE_URL = "https://world.openfoodfacts.org/"
    }

    private val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    suspend fun lookupProduct(barcode: String): ProductInformation? {
        // Respect the user's privacy choice. If they haven't opted in,
        // we never make a network call at all.
        if (!appSettings.isOnlineLookupEnabled) {
            Log.d(TAG, "Online lookup disabled by user; skipping network call.")
            return null
        }

        return try {
            val response = api.getProduct(barcode)
            if (response.status != 1 || response.product == null) {
                null
            } else {
                val p = response.product
                ProductInformation(
                    barcode = barcode,
                    name = p.product_name,
                    brand = p.brands,
                    quantity = p.quantity,
                    ingredients = p.ingredients_text,
                    allergens = p.allergens
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Online product lookup failed: ${e.message}", e)
            null
        }
    }
}