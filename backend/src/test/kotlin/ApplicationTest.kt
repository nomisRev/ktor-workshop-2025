package org.jetbrains

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.jetbrains.customers.CreateCustomer
import org.jetbrains.customers.Customer
import org.jetbrains.customers.UpdateCustomer
import org.junit.AfterClass
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun `get all data`(): Unit = runBlocking {
        val response = client.get("/customers")
        assert(response.status == HttpStatusCode.OK)
        val data = response.body<List<Customer>>()
        assertEquals(fakeData.size, data.size)
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
        val customer = client.get("/customers").body<List<Customer>>().first()

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
        val customer = client.post("/customers") {
            contentType(ContentType.Application.Json)
            setBody(CreateCustomer("A", "a@a.com"))
        }.body<Customer>()

        val response1 = client.delete("/customers/${customer.id}")
        assert(response1.status == HttpStatusCode.OK)
        assert(response1.bodyAsText() == "Data deleted successfully")

        val response2 = client.delete("/customers/${customer.id}")
        // Assertions to confirm the successful fetching of the updated Data instances
        assert(response2.status == HttpStatusCode.NotFound)
    }

    @Before
    fun initiateData() = runBlocking {
        println("Post all the fake customers")

        client.get("/customers").body<List<Customer>>().forEach { customer ->
            client.delete("/customers/${customer.id}")
        }

        fakeData.forEach { customer ->
            client.post("/customers") {
                contentType(ContentType.Application.Json)
                setBody(CreateCustomer(customer.name, customer.email))
            }
        }
    }

    companion object {
        val fakeData = mutableListOf(
            Customer(1, "Anton", "anton@jb.com", Clock.System.now()),
            Customer(2, "Leonid", "leonid@jb.com", Clock.System.now()),
            Customer(3, "Simon", "simon@jb.com", Clock.System.now()),
        )

        val app = TestApplication {
            environment {
                config = ApplicationConfig("application.yaml")
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
