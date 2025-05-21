package org.jetbrains

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.config.property
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import kotlinx.serialization.json.Json
import org.jetbrains.app.configureChatRoutes
import org.jetbrains.customers.configureCustomerRoutes
import org.jetbrains.customers.customerDataModule
import org.jetbrains.plugins.aiModule
import org.jetbrains.plugins.setupDatabase
import org.jetbrains.security.configureSecurity
import kotlin.time.Duration.Companion.minutes

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.configure() {
    setupDatabase(property("config.database"))
    configureSecurity(property("config.auth"))
    aiModule(property("config.ai"))
    customerDataModule()
    install(WebSockets) {
        pingPeriod = 1.minutes
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(SSE)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    routing {
        get("/json") {
            call.respond(mapOf("hello" to "world"))
        }
        get("/health") {
            call.respondText("Hello World!")
        }
        configureCustomerRoutes()
        configureChatRoutes()
    }
}
