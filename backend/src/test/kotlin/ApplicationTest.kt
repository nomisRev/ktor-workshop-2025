package org.jetbrains

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.customers.Booking
import org.jetbrains.customers.CreateBooking
import org.jetbrains.customers.CreateCustomer
import org.jetbrains.customers.Customer
import org.jetbrains.customers.CustomerWithBooking
import org.jetbrains.customers.UpdateCustomer
import org.junit.AfterClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ApplicationTest {
    @Test
    fun testRoot() = runBlocking {
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testJson() = runBlocking {
        val response = client.get("/json")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(mapOf("hello" to "world"), response.body<Map<String, String>>())
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createCustomer(): Customer =
        client.post("/customers") {
            contentType(ContentType.Application.Json)
            setBody(CreateCustomer(Uuid.random().toString(), "${Uuid.random()}@a.com"))
        }.body<Customer>()

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createCustomerWithBooking(): CustomerWithBooking {
        val customer = client.post("/customers") {
            contentType(ContentType.Application.Json)
            setBody(CreateCustomer(Uuid.random().toString(), "${Uuid.random()}@a.com"))
        }.body<Customer>()
        val booking = client.post("/customers/bookings/${customer.id}/create") {
            contentType(ContentType.Application.Json)
            setBody(CreateBooking(customer.id, 100.0))
        }.body<Booking>()
        return CustomerWithBooking(customer.id, customer.name, customer.email, customer.createdAt, listOf(booking))
    }

    @Test
    fun `get all data`(): Unit = runBlocking {
        val customer = createCustomerWithBooking()
        val response = client.get("/customers")
        assert(response.status == HttpStatusCode.OK)
        val data = response.body<List<CustomerWithBooking>>()
        assertTrue(data.contains(customer), "Expected $data to contain $customer")
    }

    @Test
    fun `post data instance`(): Unit = runBlocking {
        val response = client.post("/customers") {
            contentType(ContentType.Application.Json)
            setBody(CreateCustomer("A", "a@a.com"))
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `put data instance`(): Unit = runBlocking {
        val customer = createCustomer()
        val response = client.put("/customers/${customer.id}") {
            contentType(ContentType.Application.Json)
            setBody(UpdateCustomer("Mr. ${customer.name}", customer.email))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val updated = response.body<Customer>()
        assertEquals("Mr. ${customer.name}", updated.name)
        assertEquals(customer.email, updated.email)
    }

    @Test
    fun `delete data instance`(): Unit = runBlocking {
        val response1 = client.delete("/customers/1")
        assertEquals(HttpStatusCode.OK, response1.status)
        assertEquals("Data deleted successfully", response1.bodyAsText())

        val response2 = client.get("/customers/1")
        // Assertions to confirm the successful fetching of the updated Data instances
        assertEquals(HttpStatusCode.NotFound, response2.status)
    }

    companion object {
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
