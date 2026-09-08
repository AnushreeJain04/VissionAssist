package com.visionassist.app.product

class ProductManager(private val repository: ProductRepository) {

    suspend fun describeProduct(barcode: String): String {
        val product = repository.lookupProduct(barcode)

        if (product == null || product.isEmpty()) {
            return "I couldn't verify the product information."
        }

        val parts = mutableListOf<String>()

        product.name?.let { parts.add("This is $it.") }
        product.brand?.let { parts.add("Brand: $it.") }
        product.productType?.let { parts.add("Type: $it.") }
        product.quantity?.let { parts.add("Net weight: $it.") }
        product.price?.let { parts.add("Price: $it.") }
        product.manufacturingDate?.let { parts.add("Manufactured: $it.") }
        product.expiryDate?.let { parts.add("Expiry date: $it.") }
        product.ingredients?.let { parts.add("Ingredients: $it.") }
        product.allergens?.let { parts.add("Contains: $it.") }
        product.warnings?.let { parts.add("Warning: $it.") }
        product.usageInstructions?.let { parts.add("Usage: $it.") }

        return parts.joinToString(" ")
    }
}