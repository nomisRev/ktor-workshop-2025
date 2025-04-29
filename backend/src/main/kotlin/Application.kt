package org.jetbrains

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) { json() }
    routing {
        get("/json") {
            call.respond(mapOf("hello" to "world"))
        }
        get("/") {
            call.respondText("Hello World!")
        }
        configureCustomerRoutes()
    }
}

@Serializable data class CreateCustomer(val name: String, val email: String)

@Serializable data class UpdateCustomer(val name: String? = null, val email: String? = null)

@Serializable
data class Customer(val id: Int, val name: String, val email: String, val createdAt: Instant)
