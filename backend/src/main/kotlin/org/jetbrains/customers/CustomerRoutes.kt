package org.jetbrains.customers

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.provideDelegate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.delete

fun Application.configureCustomerRoutes() {
    val repository: CustomerRepository by dependencies

    routing {
        route("/customers") {
            get {
                call.respond(repository.findAll())
            }
            post {
                val newCustomer = call.receive<CreateCustomer>()
                val customer = repository.save(newCustomer)
                call.respond(HttpStatusCode.Created, customer)
            }
            get("/{customerId}") {
                call.parameters["customerId"]?.let {
                    val customer = repository.find(it.toInt())
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
                    val updated = repository.update(customerId, updatedCustomer)
                    if (updated != null) {
                        call.respond(HttpStatusCode.OK, updated)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Data to update not found")
                    }
                }
            }
            delete("/{customerId}") {
                val customerId = call.parameters["customerId"]?.toInt()
                if (customerId != null) {
                    val deleted = repository.delete(customerId)
                    if (deleted) {
                        call.respond(HttpStatusCode.OK, "Data deleted successfully")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Data to delete not found")
                    }
                }
            }
        }
    }
}
