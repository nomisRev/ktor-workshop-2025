package org.jetbrains

import kotlinx.datetime.Clock
import org.jetbrains.customers.Booking
import org.jetbrains.customers.CreateCustomer
import org.jetbrains.customers.Customer
import org.jetbrains.customers.CustomerRepository
import org.jetbrains.customers.CustomerWithBooking
import org.jetbrains.customers.UpdateCustomer
import kotlin.random.Random

class FakeCustomerRepository(private var storage: MutableList<CustomerWithBooking> = mutableListOf()) : CustomerRepository {
    private val seed = Random.Default

    override fun findAll(): List<CustomerWithBooking> = storage

    override fun save(create: CreateCustomer): Customer {
        val customer = CustomerWithBooking(seed.nextInt(), create.name, create.email, Clock.System.now(), emptyList())
        storage.add(customer)
        return Customer(customer.id, customer.name, customer.email, customer.createdAt)
    }

    override fun createBooking(id: Int, amount: Double): Booking {
        val customer = storage.single { it.id == id }
        val booking = Booking(seed.nextInt(), id, Clock.System.now(), amount)
        storage.remove(customer)
        storage.add(customer.copy(bookings = customer.bookings + booking))
        return booking
    }

    override fun delete(id: Int): Boolean {
        val customer = storage.single { it.id == id }
        return storage.remove(customer)
    }

    override fun update(
        id: Int,
        updateCustomer: UpdateCustomer
    ): Customer? {
        val customer = storage.singleOrNull { it.id == id } ?: return null
        storage.remove(customer)
        val updated = CustomerWithBooking(
            id,
            updateCustomer.name ?: customer.name,
            updateCustomer.email ?: customer.email,
            customer.createdAt,
            customer.bookings
        )
        storage.add(updated)
        return Customer(updated.id, updated.name, updated.email, updated.createdAt)
    }

    override fun find(id: Int): CustomerWithBooking? = storage.find { it.id == id }
}