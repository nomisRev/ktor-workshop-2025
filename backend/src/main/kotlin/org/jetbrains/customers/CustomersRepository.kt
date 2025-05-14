package org.jetbrains.customers

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.invoke
import io.ktor.server.plugins.di.provide
import io.ktor.server.plugins.di.resolve
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insertReturning
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.updateReturning

fun Application.customerDataModule() {
    dependencies.invoke {
        provide<CustomerRepository> { CustomerRepositoryImpl(resolve()) }
    }
}

object Customers : IntIdTable("customers", "customer_id") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

interface CustomerRepository {
    suspend fun findAll(): List<Customer>
    suspend fun save(create: CreateCustomer): Customer
    suspend fun find(id: Int): Customer?
    suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer?
    suspend fun delete(id: Int): Boolean
}

class CustomerRepositoryImpl(private val database: R2dbcDatabase) : CustomerRepository {
    override suspend fun findAll(): List<Customer> = suspendTransaction(db = database) {
        Customers.selectAll().map { it.toCustomer() }.toList()
    }

    override suspend fun save(create: CreateCustomer): Customer = suspendTransaction(db = database) {
        Customers.insertReturning {insert ->
            insert[Customers.name] = name
            insert[Customers.email] = email
        }.single().toCustomer()
    }

    override suspend fun find(id: Int): Customer? =
        suspendTransaction(db = database) {
            Customers.selectAll().where { Customers.id eq id }.singleOrNull()?.toCustomer()
        }

    override suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer? =
        suspendTransaction(db = database) {
            Customers.updateReturning(where = { Customers.id eq id }) {update ->
                if (updateCustomer.name != null) update[Customers.name] = updateCustomer.name
                if (updateCustomer.email != null) update[Customers.email] = updateCustomer.email
            }.singleOrNull()?.toCustomer()
        }

    override suspend fun delete(id: Int): Boolean =
        suspendTransaction(db= database) { Customers.deleteWhere { Customers.id eq id } > 0 }

    private fun ResultRow.toCustomer(): Customer =
        Customer(
            id = this[Customers.id].value,
            name = this[Customers.name],
            email = this[Customers.email],
            createdAt = this[Customers.createdAt],
        )
}
