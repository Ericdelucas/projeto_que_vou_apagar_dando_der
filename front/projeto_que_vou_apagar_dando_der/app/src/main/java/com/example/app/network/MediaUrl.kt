package com.example.app.network

import java.net.URI

object MediaUrl {
    fun resolve(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val base = URI(ApiClient.BASE_URL)
            val origin = URI(base.scheme, null, base.host, base.port, "/", null, null)
            val url = origin.resolve(value)
            if (base.scheme == "https" && url.scheme == "http" && url.host == base.host)
                URI("https", null, url.host, -1, url.path, url.query, url.fragment).toASCIIString()
            else url.toASCIIString()
        }.getOrNull()
    }
}
