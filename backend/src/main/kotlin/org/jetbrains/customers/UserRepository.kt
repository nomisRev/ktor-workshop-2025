package org.jetbrains.customers

interface CustomerRepository {
    fun findAll(): List<Customer>
    fun save(create: CreateCustomer): Customer
    fun find(id: Int): Customer?
    fun update(id: Int, updateCustomer: UpdateCustomer): Customer?
    fun delete(id: Int): Boolean
}

class CustomerRepositoryImpl : CustomerRepository {
    override fun findAll(): List<Customer> {
        TODO("Not yet implemented")
    }

    override fun save(create: CreateCustomer): Customer {
        TODO("Not yet implemented")
    }

    override fun find(id: Int): Customer? {
        TODO("Not yet implemented")
    }

    override fun update(id: Int, updateCustomer: UpdateCustomer): Customer? {
        TODO("Not yet implemented")
    }

    override fun delete(id: Int): Boolean {
        TODO("Not yet implemented")
    }
}
