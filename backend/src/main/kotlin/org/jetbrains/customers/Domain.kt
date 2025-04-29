package org.jetbrains.customers

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable data class CreateCustomer(val name: String, val email: String)

@Serializable data class UpdateCustomer(val name: String? = null, val email: String? = null)

@Serializable
data class Customer(val id: Int, val name: String, val email: String, val createdAt: Instant)
