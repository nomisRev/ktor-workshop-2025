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

class ApplicationTest {
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
            setBody(CreateCustomer("A", "a@a.com"))
        }
        assert(response.status == HttpStatusCode.Created)
        val customer = response.body<Customer>()
        assert(customer.name == "A")
        assert(customer.email == "a@a.com")
    }

    @Test
    fun `put data instance`(): Unit = runBlocking {
        val customer = fakeData.first()
        val response = client.put("/customers/${customer.id}") {
            contentType(ContentType.Application.Json)
            setBody(UpdateCustomer(name = "Mr. ${customer.name}"))
        }
        assert(response.status == HttpStatusCode.OK)
        val updated = response.body<Customer>()
        assert(updated.name == "Mr. ${customer.name}")
        assert(updated.email == customer.email)
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
