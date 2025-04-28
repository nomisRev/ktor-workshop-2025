package org.jetbrains

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assert(response.status == HttpStatusCode.OK)
    }

    @Test
    fun testJson() = testApplication {
        application {
            module()
        }
        val response = createClient {
            install(ContentNegotiation) { json() }
        }.get("/json")
        assert(response.status == HttpStatusCode.OK)
        assert(response.body<Map<String, String>>() == mapOf("hello" to "world"))
    }
}
