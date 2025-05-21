package org.jetbrains.customers

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable data class CreateCustomer(val name: String, val email: String)

@Serializable data class UpdateCustomer(val name: String? = null, val email: String? = null)

@Serializable data class CreateBooking(val customerId: Int, val amount: Double)

@Serializable
data class Booking(val id: Int, val customerId: Int, val bookingDate: Instant, val amount: Double)

@Serializable
data class Customer(
    val id: Int,
    val name: String,
    val email: String,
    val createdAt: Instant
)

@Serializable
data class CustomerWithBooking(
    val id: Int,
    val name: String,
    val email: String,
    val createdAt: Instant,
    val bookings: List<Booking>
)
