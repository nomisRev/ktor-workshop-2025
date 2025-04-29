package org.jetbrains

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.application.install
import io.ktor.server.config.property
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.invoke
import io.ktor.server.plugins.di.provide
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.invoke
import io.ktor.server.plugins.di.provide
import io.ktor.server.routing.routing
import org.jetbrains.customers.CustomerRepository
import org.jetbrains.customers.CustomerRepositoryImpl
import org.jetbrains.customers.configureCustomerRoutes
import org.jetbrains.customers.customerDataModule
import org.jetbrains.exposed.sql.Database
import org.jetbrains.plugins.DbConfig
import org.jetbrains.plugins.setupDatabase

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.configure() {
    setupDatabase(property<DbConfig>("config.database"))
    customerDataModule()
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    routing {
        configureCustomerRoutes()
    }
}
