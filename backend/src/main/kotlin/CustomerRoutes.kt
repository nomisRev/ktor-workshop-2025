package org.jetbrains

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

//FIXME: this is a hack!
var customers = mutableListOf<Customer>()

fun Application.configureCustomerRoutes() {
    routing {
        route("/customers") {
            get {
                call.respond(customers)
            }
            post {
                val user = call.receive<Customer>()
                customers.add(user)
                call.respond(HttpStatusCode.Created, "Data added successfully")
            }
            get("/{customerId}") {
                val userId = call.parameters["customerId"]?.toInt() // FIXME: this is a bit ugly
                val user = customers.find { it.id == userId }
                if (user != null) {
                    call.respond(user)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            put("/{customerId}") {
                val userId = call.parameters["customerId"]?.toInt()
                val updatedUser = call.receive<Customer>()
                val oldUser = customers.find { it.id == userId }
                if (oldUser != null) {
                    customers.remove(oldUser)
                    customers.add(updatedUser)
                    call.respond(HttpStatusCode.OK, "Data updated successfully")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Data to update not found")
                }
            }
            delete("/{customerId}") {
                val userId = call.parameters["customerId"]?.toInt()
                val deleted = customers.find { it.id == userId }
                if (deleted != null) {
                    customers.remove(deleted)
                    call.respond(HttpStatusCode.OK, "Data deleted successfully")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Data to delete not found")
                }
            }
        }
    }


}
