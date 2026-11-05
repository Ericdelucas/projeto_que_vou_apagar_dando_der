package com.example.app.network

import java.net.URI

object ApiEndpoint {
    fun normalize(value: String, allowLocal: Boolean): String {
        val uri = URI(value.trim())
        require(!uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null)
        val local = uri.host in setOf("localhost", "10.0.2.2")
        require(uri.scheme == "https" || (allowLocal && local && uri.scheme == "http"))
        require(uri.port == -1 || uri.port in 1..65535)
        require(uri.path in listOf("", "/", "/api", "/api/"))
        return URI(uri.scheme, null, uri.host, uri.port, "/api/", null, null).toASCIIString()
    }
}
