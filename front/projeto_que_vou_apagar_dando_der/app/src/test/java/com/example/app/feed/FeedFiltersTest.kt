package com.example.app.feed

import com.example.app.network.*
import org.junit.Assert.*
import org.junit.Test

class FeedFiltersTest {
    private val product = ProductTag(1, "Farol", "100.00", null, 1, "Iluminação", "Gol", "Palmas")
    private val linked = PostResponse(1, 1, PostAuthor(1, "Oficina"), 1, null, "Peça nova", 0, 0, product)
    private val photo = PostResponse(2, 2, PostAuthor(2, "Ana"), null, null, "Meu projeto", 0, 0, null, "https://example.com/p.jpg")
    @Test fun combinesFiltersAndKeepsUnlinkedPhotosWhenCleared() {
        val posts = listOf(linked, photo)
        assertEquals(listOf(linked), FeedFilters.apply(posts, "Gol", "Iluminação", "Farol", "iluminacao"))
        assertTrue(FeedFilters.apply(posts, "Onix", null, null, "").isEmpty())
        assertEquals(posts, FeedFilters.apply(posts, null, null, null, ""))
        assertEquals(listOf(photo), FeedFilters.apply(posts, null, null, null, "ana"))
    }
}
