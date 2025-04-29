package org.jetbrains.customers

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Customer(val id: Int, val name: String, val email: String, val createdAt: Instant)
