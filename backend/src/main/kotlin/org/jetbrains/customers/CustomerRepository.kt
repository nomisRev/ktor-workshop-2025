package org.jetbrains.customers

interface CustomerRepository {
    suspend fun findAll(): List<Customer>
    suspend fun save(create: CreateCustomer): Customer
    suspend fun find(id: Int): Customer?
    suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer?
    suspend fun delete(id: Int): Boolean
}

class CustomerRepositoryImpl : CustomerRepository {
    override suspend fun findAll(): List<Customer> {
        TODO("Not yet implemented")
    }

    override suspend fun save(create: CreateCustomer): Customer {
        TODO("Not yet implemented")
    }

    override suspend fun find(id: Int): Customer? {
        TODO("Not yet implemented")
    }

    override suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer? {
        TODO("Not yet implemented")
    }

    override suspend fun delete(id: Int): Boolean {
        TODO("Not yet implemented")
    }
}
