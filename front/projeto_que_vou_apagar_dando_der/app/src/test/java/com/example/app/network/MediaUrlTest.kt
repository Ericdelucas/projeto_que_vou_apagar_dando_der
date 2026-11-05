package com.example.app.network

import org.junit.Assert.*
import org.junit.Test

class MediaUrlTest {
    @Test fun upgradesOnlyOwnServerAndResolvesUploads() {
        assertEquals("https://skippecas-api.onrender.com/uploads/a.jpg", MediaUrl.resolve("http://skippecas-api.onrender.com/uploads/a.jpg"))
        assertEquals("https://skippecas-api.onrender.com/uploads/a.jpg", MediaUrl.resolve("/uploads/a.jpg"))
        assertEquals("https://other.example/a.jpg", MediaUrl.resolve("https://other.example/a.jpg"))
        assertNull(MediaUrl.resolve(""))
    }
}
