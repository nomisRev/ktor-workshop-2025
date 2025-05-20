@file:JvmName("CustomerRepositoryKt")

package org.jetbrains.customers

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

fun Application.customerDataModule() {
    dependencies {
        provide<CustomerRepository> { CustomerRepositoryImpl(resolve()) }
    }
}

object Customers : IntIdTable("customers", "customer_id") {
    val name = varchar("NAME", 255)
    val email = varchar("EMAIL", 255).uniqueIndex()
    val createdAt = timestamp("CREATED_AT").defaultExpression(CurrentTimestamp)
}

interface CustomerRepository {
    suspend fun findAll(): List<Customer>
    suspend fun save(create: CreateCustomer): Customer
    suspend fun find(id: Int): Customer?
    suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer?
    suspend fun delete(id: Int): Boolean
}

class CustomerRepositoryImpl(private val database: R2dbcDatabase) : CustomerRepository {
    override suspend fun findAll(): List<Customer> =  suspendTransaction(Dispatchers.IO, db = database) {
        Customers.selectAll().map { it.toCustomer() }.toList()
    }

    override suspend fun save(create: CreateCustomer): Customer =
        suspendTransaction(Dispatchers.IO, db = database) {
            val id = Customers.insertAndGetId { insert ->
                insert[Customers.name] = create.name
                insert[Customers.email] = create.email
            }
            Customers.selectAll().where { Customers.id eq id }.single().toCustomer()
        }

    override suspend fun find(id: Int): Customer? = suspendTransaction(Dispatchers.IO, db = database) {
        Customers.selectAll().where { Customers.id eq id }.singleOrNull()?.toCustomer()
    }

    override suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer? =
        suspendTransaction(Dispatchers.IO, db = database) {
            Customers.update(where = { Customers.id eq id }) { update ->
                if (updateCustomer.name != null) update[Customers.name] = updateCustomer.name
                if (updateCustomer.email != null) update[Customers.email] = updateCustomer.email
            }

            Customers.selectAll().where { Customers.id eq id }.singleOrNull()?.toCustomer()
        }

    override suspend fun delete(id: Int): Boolean = suspendTransaction(Dispatchers.IO, db = database) {
        Customers.deleteWhere { Customers.id eq id } > 0
    }

    private fun ResultRow.toCustomer(): Customer =
        Customer(
            id = this[Customers.id].value,
            name = this[Customers.name],
            email = this[Customers.email],
            createdAt = this[Customers.createdAt],
        )
}
