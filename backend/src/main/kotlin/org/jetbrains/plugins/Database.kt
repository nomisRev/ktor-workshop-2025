package org.jetbrains.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.invoke
import io.ktor.server.plugins.di.provide
import kotlinx.serialization.Serializable
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager

@Serializable
data class DbConfig(
    val driverClassName: String,
    val url: String,
    val username: String,
    val password: String,
    val flyway: FlywayConfig
)

@Serializable
data class FlywayConfig(val locations: String, val baselineOnMigrate: Boolean)

fun Application.setupDatabase(config: DbConfig) {
    flyway(config)
    val database = R2dbcDatabase.connect("r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1")

    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
    }

    dependencies {
        provide<R2dbcDatabase> { database }
    }
}

fun flyway(config: DbConfig): MigrateResult =
    Flyway.configure()
        .dataSource(config.url, config.username, config.password)
        .locations(config.flyway.locations)
        .baselineOnMigrate(config.flyway.baselineOnMigrate)
        .load()
        .migrate()
