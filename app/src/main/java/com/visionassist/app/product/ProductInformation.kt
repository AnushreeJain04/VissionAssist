package com.visionassist.app.product

/**
 * Represents everything we might know about a scanned product.
 * Any field can be null/empty if that information isn't available —
 * we never invent values to fill gaps.
 */
data class ProductInformation(
    val barcode: String,
    val name: String? = null,
    val brand: String? = null,
    val productType: String? = null,
    val quantity: String? = null,
    val price: String? = null,
    val manufacturingDate: String? = null,
    val expiryDate: String? = null,
    val ingredients: String? = null,
    val allergens: String? = null,
    val warnings: String? = null,
    val usageInstructions: String? = null
) {
    /**
     * True if we found essentially nothing useful for this barcode.
     */
    fun isEmpty(): Boolean {
        return name == null && brand == null && productType == null &&
                quantity == null && price == null && manufacturingDate == null &&
                expiryDate == null && ingredients == null && allergens == null &&
                warnings == null && usageInstructions == null
    }
}