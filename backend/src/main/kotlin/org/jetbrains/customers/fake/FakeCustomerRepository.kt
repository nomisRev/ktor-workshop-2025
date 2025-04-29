package org.jetbrains.customers.fake

import kotlinx.datetime.Clock
import org.jetbrains.customers.CreateCustomer
import org.jetbrains.customers.Customer
import org.jetbrains.customers.CustomerRepository
import org.jetbrains.customers.UpdateCustomer
import kotlin.random.Random

class FakeCustomerRepository(private var storage: MutableList<Customer> = mutableListOf()) : CustomerRepository {
    private val seed = Random.Default

    override fun findAll(): List<Customer> = storage

    override fun save(create: CreateCustomer): Customer {
        val customer = Customer(seed.nextInt(), create.name, create.email, Clock.System.now())
        storage.add(customer)
        return customer
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
        val updated = Customer(
            id,
            updateCustomer.name ?: customer.name,
            updateCustomer.email ?: customer.email,
            customer.createdAt
        )
        storage.add(updated)
        return updated
    }

    override fun find(id: Int): Customer? = storage.find { it.id == id }
}
