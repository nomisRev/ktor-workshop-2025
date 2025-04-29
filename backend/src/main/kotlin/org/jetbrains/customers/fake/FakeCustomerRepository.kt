package org.jetbrains.customers.fake

import org.jetbrains.customers.Customer
import org.jetbrains.customers.CustomerRepository

class FakeCustomerRepository(private var storage: MutableList<Customer> = mutableListOf()) : CustomerRepository {

    override fun findAll(): List<Customer> = storage

    override fun save(user: Customer): Boolean = storage.add(user)

    override fun find(id: Int): Customer? = storage.find { it.id == id }

    override fun delete(user: Customer): Boolean = storage.remove(user)
}
