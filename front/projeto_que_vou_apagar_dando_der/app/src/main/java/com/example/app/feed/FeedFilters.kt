package com.example.app.feed

import com.example.app.network.PostResponse
import java.text.Normalizer

object FeedFilters {
    private fun fold(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD).replace("\\p{M}+".toRegex(), "").lowercase(java.util.Locale.ROOT)
    fun apply(posts: List<PostResponse>, model: String?, category: String?, piece: String?, query: String): List<PostResponse> = posts.filter {
        (model == null || it.product?.model == model) &&
        (category == null || it.product?.category == category) &&
        (piece == null || it.product?.title == piece) &&
        (query.isBlank() || fold(listOfNotNull(it.description, it.user.name, it.product?.title, it.product?.model, it.product?.category).joinToString(" ")).contains(fold(query.trim())))
    }
}
