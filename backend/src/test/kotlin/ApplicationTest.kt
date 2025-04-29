package org.jetbrains

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.AfterClass
import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun `get all data`(): Unit = runBlocking {
        val response = client.get("/customers")
        assert(response.status == HttpStatusCode.OK)
        val data = response.body<List<Customer>>()
        assert(data.size == fakeData.size)
    }

    @Test
    fun `post data instance`(): Unit = runBlocking {
        val response = client.post("/customers") {
            contentType(ContentType.Application.Json)
            setBody(Customer(123, "A", "a@a.com", Clock.System.now()))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("Data added successfully", response.bodyAsText())
    }

    @Test
    fun `put data instance`(): Unit = runBlocking {
        val user = fakeData.first()
        val updatedDataResponse = client.put("/customers/${user.id}") {
            contentType(ContentType.Application.Json)
            val customer = fakeData.first()
            setBody(customer.copy(name = "Mr. ${customer.name}"))
        }
        assertEquals(HttpStatusCode.OK, updatedDataResponse.status)
        assertEquals("Data updated successfully", updatedDataResponse.bodyAsText())
    }

    @Test
    fun `delete data instance`(): Unit = runBlocking {
        val response1 = client.delete("/customers/1")
        assert(response1.status == HttpStatusCode.OK)
        assert(response1.bodyAsText() == "Data deleted successfully")

        val response2 = client.get("/customers/1")
        // Assertions to confirm the successful fetching of the updated Data instances
        assert(response2.status == HttpStatusCode.NotFound)
    }

    companion object {
        val fakeData = mutableListOf(
            Customer(1, "Anton", "anton@jb.com", Clock.System.now()),
            Customer(2, "Leonid", "leonid@jb.com", Clock.System.now()),
            Customer(3, "Simon", "simon@jb.com", Clock.System.now()),
        )

        val app = TestApplication {
            application {
                module()
                customers = fakeData //FIXME: this is another hack
            }
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
