package org.jetbrains.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.invoke
import io.ktor.server.plugins.di.provide
import kotlinx.serialization.Serializable
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import javax.sql.DataSource

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
    val dataSource = dataSource(config)
    flyway(dataSource, config.flyway)
    val database = Database.connect(dataSource)

    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
        dataSource.close()
    }

    dependencies {
        provide<Database> { database }
    }
}

fun flyway(dataSource: DataSource, flywayConfig: FlywayConfig): MigrateResult =
    Flyway.configure()
        .dataSource(dataSource)
        .locations(flywayConfig.locations)
        .baselineOnMigrate(true)
        .load()
        .migrate()

fun dataSource(config: DbConfig): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            poolName = "Database Pool"
            maximumPoolSize = 20
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

            jdbcUrl = config.url
            username = config.username
            password = config.password
            driverClassName = config.driverClassName
        }
    )
