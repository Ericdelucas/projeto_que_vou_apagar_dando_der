package com.example.app.seller
import com.example.app.network.SellerProduct
object SellerFilters {
    fun apply(products: List<SellerProduct>, model: String?, category: String?, piece: String?) = products.filter {
        (model == null || it.model == model) && (category == null || it.category == category) && (piece == null || it.title == piece)
    }
}
