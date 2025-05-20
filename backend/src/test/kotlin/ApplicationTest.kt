package org.jetbrains

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.jetbrains.customers.CreateCustomer
import org.jetbrains.customers.Customer
import org.jetbrains.customers.CustomerRepository
import org.jetbrains.customers.UpdateCustomer
import org.jetbrains.customers.fake.FakeCustomerRepository
import org.junit.AfterClass
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = runBlocking {
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testJson() = runBlocking {
        val response = client.get("/json")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(mapOf("hello" to "world"), response.body<Map<String, String>>())
    }

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
        assertEquals(HttpStatusCode.Created, response.status)

    }

    @Test
    fun `put data instance`(): Unit = runBlocking {
        val customer = fakeData.first()
        val response = client.put("/customers/${customer.id}") {
            contentType(ContentType.Application.Json)
            setBody(UpdateCustomer("Mr. ${customer.name}", customer.email))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val updated = response.body<Customer>()
        assertEquals( "Mr. ${customer.name}", updated.name)
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
        val fakeData = mutableListOf(
            Customer(1, "Anton", "anton@jb.com", Clock.System.now()),
            Customer(2, "Leonid", "leonid@jb.com", Clock.System.now()),
            Customer(3, "Simon", "simon@jb.com", Clock.System.now()),
        )

        val app = TestApplication {
            application {
                dependencies {
                    provide<CustomerRepository> { FakeCustomerRepository(fakeData) }
                }
                module()
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
