package org.jetbrains

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlin.random.Random

//FIXME: this is a hack!
var customers = mutableListOf<Customer>()

fun Application.configureCustomerRoutes() {
    routing {
        route("/customers") {
            get {
                call.respond(customers)
            }
            post {
                val newCustomer = call.receive<CreateCustomer>()
                val customer = Customer(Random.nextInt(), newCustomer.name, newCustomer.email, Clock.System.now())
                customers.add(customer)
                call.respond(HttpStatusCode.Created, customer)
            }
            get("/{customerId}") {
                call.parameters["customerId"]?.let { customerId ->
                    val customer = customers.find { it.id == customerId.toInt() }
                    if (customer != null) {
                        call.respond(customer)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
            put("/{customerId}") {
                val customerId = call.parameters["customerId"]?.toInt()
                val updatedCustomer = call.receive<UpdateCustomer>()
                if (customerId != null) {
                    val oldCustomer = customers.find { it.id == customerId }
                    if (oldCustomer != null) {
                        customers.remove(oldCustomer)
                        val updated = Customer(
                            oldCustomer.id,
                            updatedCustomer.name ?: oldCustomer.name,
                            updatedCustomer.email ?: oldCustomer.email,
                            oldCustomer.createdAt
                        )
                        customers.add(updated)
                        call.respond(HttpStatusCode.OK, updated)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Data to update not found")
                    }
                }
            }
            delete("/{customerId}") {
                val customerId = call.parameters["customerId"]?.toInt()
                if (customerId != null) {
                    val deleted = customers.find { it.id == customerId }
                    if (deleted != null) {
                        customers.remove(deleted)
                        call.respond(status = HttpStatusCode.OK, message = "Data deleted successfully")
                    } else {
                        call.respond(status = HttpStatusCode.NotFound, message = "Data to delete not found")
                    }
                }
            }
        }
    }
}
