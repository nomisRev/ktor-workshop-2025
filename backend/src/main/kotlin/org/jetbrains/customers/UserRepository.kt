package org.jetbrains.customers

interface CustomerRepository {
    fun findAll(): List<Customer>
    fun save(user: Customer): Boolean
    fun find(id: Int): Customer?
    fun delete(user: Customer): Boolean
}

class CustomerRepositoryImpl : CustomerRepository {
    override fun findAll(): List<Customer> {
        TODO("Not yet implemented")
    }

    override fun save(user: Customer): Boolean {
        TODO("Not yet implemented")
    }

    override fun find(id: Int): Customer? {
        TODO("Not yet implemented")
    }

    override fun delete(user: Customer): Boolean {
        TODO("Not yet implemented")
    }
}
