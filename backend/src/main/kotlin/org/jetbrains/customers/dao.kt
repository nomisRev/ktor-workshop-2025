package org.jetbrains.customers

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

typealias IntId = EntityID<Int>

class CustomerDAO(id: IntId) : IntEntity(id) {
    companion object : IntEntityClass<CustomerDAO>(Customers)

    var name by Customers.name
    var email by Customers.email
    var createdAt by Customers.createdAt
    val bookings by BookingsDAO referrersOn Bookings.customerId

    fun toCustomer() = Customer(
        id = id.value,
        name = name,
        email = email,
        createdAt = createdAt,
    )

    fun toCustomerWithBooking() = CustomerWithBooking(
        id = id.value,
        name = name,
        email = email,
        createdAt = createdAt,
        bookings = bookings.map { it.toBooking() }
    )

    override fun toString(): String = "CustomerDAO(name='$name', email='$email')"
}

class BookingsDAO(id: IntId) : IntEntity(id) {
    companion object : IntEntityClass<BookingsDAO>(Bookings)

    var bookingDate by Bookings.bookingDate
    var amount by Bookings.amount
    var customer by CustomerDAO referencedOn Bookings.customerId

    fun toBooking() = Booking(
        id = id.value,
        customerId = customer.id.value,
        bookingDate = bookingDate,
        amount = amount
    )

    override fun toString(): String = "BookingsDAO(bookingDate=$bookingDate, amount=$amount)"
}
