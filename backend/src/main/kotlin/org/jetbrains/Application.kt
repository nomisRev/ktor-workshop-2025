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
import org.jetbrains.customers.CustomerRepository
import org.jetbrains.customers.configureCustomerRoutes
import org.jetbrains.customers.fake.FakeCustomerRepository

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val repository = FakeCustomerRepository()
    configureRoutes(repository)
}

fun Application.configureRoutes(repository: CustomerRepository) {
    install(ContentNegotiation) { json() }
    routing {
        get("/json") {
            call.respond(mapOf("hello" to "world"))
        }
        get("/") {
            call.respondText("Hello World!")
        }
        configureCustomerRoutes(repository)
    }
}
