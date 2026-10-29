package com.example.app.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthApiTest {
    @Test
    fun loginSendsExpectedPayloadAndReadsToken() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json")
                .setBody("""{"token":"jwt-teste","user":{"id":1,"name":"Ana","email":"ana@example.com","role":"comprador"}}"""))
            val api = Retrofit.Builder().baseUrl(server.url("/api/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(AuthApi::class.java)
            val response = api.login(LoginRequest("ana@example.com", "senha-123")).execute()
            assertTrue(response.isSuccessful)
            assertEquals("jwt-teste", response.body()?.token)
            assertEquals("Ana", response.body()?.user?.name)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/auth/login", request.path)
            assertEquals("""{"email":"ana@example.com","password":"senha-123"}""", request.body.readUtf8())
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun registrationSendsAccountAndReadsCreatedUser() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"user":{"id":4,"name":"Eric","email":"eric@example.com","role":"vendedor"}}"""))
            val api = Retrofit.Builder().baseUrl(server.url("/api/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(AuthApi::class.java)
            val response = api.register(RegisterRequest("Eric", "eric@example.com", "senha-123", "vendedor")).execute()
            assertEquals(201, response.code())
            assertEquals("vendedor", response.body()?.user?.role)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/auth/register", request.path)
            val payload = com.google.gson.JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertEquals("Eric", payload.get("name").asString)
            assertEquals("eric@example.com", payload.get("email").asString)
            assertEquals("senha-123", payload.get("password").asString)
            assertEquals("vendedor", payload.get("role").asString)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun rejectedCredentialsDoNotProduceAToken() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(401)
                .setBody("""{"message":"E-mail ou senha inválidos."}"""))
            val api = Retrofit.Builder().baseUrl(server.url("/api/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
                .create(AuthApi::class.java)
            val response = api.login(LoginRequest("ana@example.com", "senha-errada")).execute()
            assertEquals(401, response.code())
            assertNull(response.body())
            response.errorBody()?.close()
        } finally {
            server.shutdown()
        }
    }
}
