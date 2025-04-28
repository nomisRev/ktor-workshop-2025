package org.jetbrains

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.AfterClass
import org.junit.ClassRule
import kotlin.test.Test

class ApplicationTest {
    @Test
    fun testRoot() = runBlocking {
        val response = client.get("/")
        assert(response.status == HttpStatusCode.OK)
    }

    @Test
    fun testJson() = runBlocking {
        val response = client.get("/json")
        assert(response.status == HttpStatusCode.OK)
        assert(response.body<Map<String, String>>() == mapOf("hello" to "world"))
    }

    val fakeData = mutableListOf(
        Customer(1, "Anton", "anton@jb.com", Clock.System.now()),
        Customer(2, "Leonid", "leonid@jb.com", Clock.System.now()),
        Customer(3, "Simon", "simon@jb.com", Clock.System.now()),
    )

    @Test
    fun `get all data`(): Unit = runBlocking {

    }

    @Test
    fun `post data instance`(): Unit = runBlocking {

    }

    @Test
    fun `put data instance`(): Unit = runBlocking {

    }

    @Test
    fun `delete data instance`(): Unit = runBlocking {

    }

    companion object {
        val app = TestApplication {
            application { module() }
        }

        val client = app.createClient {
            install(ContentNegotiation) { json() }
        }

        @AfterClass
        @JvmStatic
        fun close() = runBlocking {
            app.stop()
        }
    }
}
