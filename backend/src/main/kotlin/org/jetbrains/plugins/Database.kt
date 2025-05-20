package org.jetbrains.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.serialization.*
import org.flywaydb.core.*
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import javax.sql.DataSource

@Serializable
data class DbConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val flyway: FlywayConfig
)

@Serializable
data class FlywayConfig(val locations: String, val baselineOnMigrate: Boolean)

fun Application.setupDatabase(config: DbConfig) {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.username
        password = config.password
    })

    flyway(dataSource, config)
    val database = Database.connect(dataSource)

    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
    }

    dependencies {
        provide<Database> { database }
    }
}

fun flyway(dataSource: DataSource, config: DbConfig): MigrateResult =
    Flyway.configure()
        .dataSource(dataSource)
        .locations(config.flyway.locations)
        .baselineOnMigrate(config.flyway.baselineOnMigrate)
        .load()
        .migrate()
