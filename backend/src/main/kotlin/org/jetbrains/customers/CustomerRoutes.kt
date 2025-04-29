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
                val customer = call.receive<Customer>()
                repository.save(customer)
                call.respond(HttpStatusCode.Created, "Data added successfully")
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

                val updatedCustomer = call.receive<Customer>()
                if (customerId != null) {
                    val oldCustomer = repository.find(customerId)
                    if (oldCustomer != null) {
                        repository.delete(oldCustomer)
                        repository.save(updatedCustomer)
                        call.respond(status = HttpStatusCode.OK, message = "Data updated successfully")
                    } else {
                        call.respond(status = HttpStatusCode.NotFound, message = "Data to update not found")
                    }
                }
            }
            delete("/{customerId}") {
                val customerId = call.parameters["customerId"]?.toInt()
                if (customerId != null) {
                    val deleted = repository.find(customerId)
                    if (deleted != null) {
                        repository.delete(deleted)
                        call.respond(status = HttpStatusCode.OK, message = "Data deleted successfully")
                    } else {
                        call.respond(status = HttpStatusCode.NotFound, message = "Data to delete not found")
                    }
                }
            }
        }
    }
}
