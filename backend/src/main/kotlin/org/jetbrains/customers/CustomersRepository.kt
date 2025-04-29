package org.jetbrains.customers

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.invoke
import io.ktor.server.plugins.di.provide
import io.ktor.server.plugins.di.resolve
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.updateReturning

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
    fun findAll(): List<Customer>
    fun save(create: CreateCustomer): Customer
    fun find(id: Int): Customer?
    fun update(id: Int, updateCustomer: UpdateCustomer): Customer?
    fun delete(id: Int): Boolean
}

class CustomerRepositoryImpl(private val database: Database) : CustomerRepository {
    override fun findAll(): List<Customer> = transaction(database) {
        Customers.selectAll().map { it.toCustomer() }
    }

    override fun save(create: CreateCustomer): Customer = transaction(database) {
        Customers.insertReturning {insert ->
            insert[Customers.name] = name
            insert[Customers.email] = email
        }.single().toCustomer()
    }

    override fun find(id: Int): Customer? =
        transaction(database) {
            Customers.selectAll().where { Customers.id eq id }.singleOrNull()?.toCustomer()
        }

    override fun update(id: Int, updateCustomer: UpdateCustomer): Customer? =
        transaction(database) {
            Customers.updateReturning(where = { Customers.id eq id }) {update ->
                if (updateCustomer.name != null) update[Customers.name] = updateCustomer.name
                if (updateCustomer.email != null) update[Customers.email] = updateCustomer.email
            }.singleOrNull()?.toCustomer()
        }

    override fun delete(id: Int): Boolean =
        transaction(database) { Customers.deleteWhere { Customers.id eq id } > 0 }

    private fun ResultRow.toCustomer(): Customer =
        Customer(
            id = this[Customers.id].value,
            name = this[Customers.name],
            email = this[Customers.email],
            createdAt = this[Customers.createdAt],
        )
}
