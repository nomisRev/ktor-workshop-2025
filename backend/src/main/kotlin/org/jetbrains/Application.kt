package org.jetbrains

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.config.property
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import org.jetbrains.app.configureChatRoutes
import org.jetbrains.customers.configureCustomerRoutes
import org.jetbrains.customers.customerDataModule
import org.jetbrains.plugins.aiModule
import org.jetbrains.plugins.setupDatabase
import org.jetbrains.security.configureSecurity

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.configure() {
    setupDatabase(property("database"))
    configureSecurity(property("auth"))
    aiModule(property("ai"))
    customerDataModule()
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    routing {
        configureCustomerRoutes()
        configureChatRoutes()
    }
}
