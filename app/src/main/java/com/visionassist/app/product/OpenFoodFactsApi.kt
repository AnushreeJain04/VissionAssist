package com.visionassist.app.product

import retrofit2.http.GET
import retrofit2.http.Path

data class OpenFoodFactsResponse(
    val status: Int,
    val product: OpenFoodFactsProduct?
)

data class OpenFoodFactsProduct(
    val product_name: String?,
    val brands: String?,
    val quantity: String?,
    val ingredients_text: String?,
    val allergens: String?
)

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponse
}