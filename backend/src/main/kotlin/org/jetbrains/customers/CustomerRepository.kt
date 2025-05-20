@file:JvmName("CustomerRepositoryKt")

package org.jetbrains.customers

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

fun Application.customerDataModule() {
    dependencies {
        provide<CustomerRepository> { CustomerRepositoryImpl(resolve()) }
    }
}

object Customers : IntIdTable("CUSTOMERS", "CUSTOMER_ID") {
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

class CustomerRepositoryImpl(private val database: Database) : CustomerRepository {
    override suspend fun findAll(): List<Customer> = transaction(db = database) {
        Customers.selectAll().map { it.toCustomer() }.toList()
    }

    override suspend fun save(create: CreateCustomer): Customer = transaction(db = database) {
        val id = Customers.insertAndGetId { insert ->
            insert[Customers.name] = create.name
            insert[Customers.email] = create.email
        }
        Customers.selectAll().where { Customers.id eq id }.single().toCustomer()
    }

    override suspend fun find(id: Int): Customer? =
        transaction(db = database) {
            Customers.selectAll().where { Customers.id eq id }.singleOrNull()?.toCustomer()
        }

    override suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer? =
        transaction(db = database) {
            Customers.update(where = { Customers.id eq id }) { update ->
                if (updateCustomer.name != null) update[Customers.name] = updateCustomer.name
                if (updateCustomer.email != null) update[Customers.email] = updateCustomer.email
            }
            Customers.selectAll().where { Customers.id eq id }.singleOrNull()?.toCustomer()
        }

    override suspend fun delete(id: Int): Boolean =
        transaction(db = database) { Customers.deleteWhere { Customers.id eq id } > 0 }

    private fun ResultRow.toCustomer(): Customer =
        Customer(
            id = this[Customers.id].value,
            name = this[Customers.name],
            email = this[Customers.email],
            createdAt = this[Customers.createdAt],
        )
}
