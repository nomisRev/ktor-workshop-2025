package org.jetbrains.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import io.r2dbc.spi.*
import kotlinx.serialization.*
import org.flywaydb.core.*
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting

@Serializable
data class DbConfig(
    val username: String,
    val password: String,
    val flyway: FlywayConfig
)

@Serializable
data class FlywayConfig(val locations: String, val baselineOnMigrate: Boolean)

fun Application.setupDatabase(config: DbConfig) {
    val file = createTempFile("h2db", ".db")
    val path = file.absolutePathString()
    flyway(path, config)

    val database = R2dbcDatabase.connect(R2dbcDatabaseConfig {
        connectionFactoryOptions = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "h2")
            .option(ConnectionFactoryOptions.PROTOCOL, "file")
            .option(ConnectionFactoryOptions.USER, config.username)
            .option(ConnectionFactoryOptions.PASSWORD, config.password)
            .option(ConnectionFactoryOptions.DATABASE, path)
            .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            .build()
    })

    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
        file.deleteExisting()
    }

    dependencies {
        provide<R2dbcDatabase> { database }
    }
}

fun flyway(path: String, config: DbConfig): MigrateResult =
    Flyway.configure()
        .dataSource("jdbc:h2:file:$path;DB_CLOSE_DELAY=-1", config.username, config.password)
        .locations(config.flyway.locations)
        .baselineOnMigrate(config.flyway.baselineOnMigrate)
        .load()
        .migrate()
