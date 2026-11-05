package com.example.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiEndpointTest {
    @Test fun apkUsesPublishedApiWithoutManualConfiguration() {
        assertEquals("https://skippecas-api.onrender.com/api/", ApiClient.BASE_URL)
    }

    @Test fun renderUrlReceivesApiPath() {
        assertEquals("https://skippecas.example/api/", ApiEndpoint.normalize(" https://skippecas.example ", false))
        assertEquals("https://skippecas.example/api/", ApiEndpoint.normalize("https://skippecas.example/api", false))
    }

    @Test fun localHttpIsRestrictedToDebug() {
        assertEquals("http://localhost:3000/api/", ApiEndpoint.normalize("http://localhost:3000", true))
        assertThrows(IllegalArgumentException::class.java) { ApiEndpoint.normalize("http://localhost:3000", false) }
        assertThrows(IllegalArgumentException::class.java) { ApiEndpoint.normalize("http://public.example", true) }
    }

    @Test fun rejectsCredentialsQueryAndUnexpectedPaths() {
        for (url in listOf("https://user:password@api.example", "https://api.example?token=secret", "https://api.example/other", "https://api.example#fragment")) {
            assertThrows(IllegalArgumentException::class.java) { ApiEndpoint.normalize(url, true) }
        }
    }
}
