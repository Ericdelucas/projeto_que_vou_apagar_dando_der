package com.example.app.seller
import com.example.app.network.SellerProduct
import org.junit.Assert.*
import org.junit.Test
class SellerFiltersTest {
    @Test fun combinesModelCategoryAndPieceAndClearsSelection() {
        val a = SellerProduct(1, "Volante", "100", 2, null, null, "Interior", "Gol", "Palmas", "ativo", 0, 0, 0, 0)
        val b = a.copy(id = 2, title = "Farol", category = "Iluminação", model = "Onix")
        assertEquals(listOf(a), SellerFilters.apply(listOf(a,b), "Gol", "Interior", "Volante"))
        assertTrue(SellerFilters.apply(listOf(a,b), "Gol", "Iluminação", null).isEmpty())
        assertEquals(listOf(a,b), SellerFilters.apply(listOf(a,b), null, null, null))
    }
}
