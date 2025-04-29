package org.jetbrains.plugins

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
    val driverClassName: String,
    val url: String,
    val username: String,
    val password: String,
    val flyway: FlywayConfig
)

@Serializable
data class FlywayConfig(val locations: String, val baselineOnMigrate: Boolean)
