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
    dependencies {
        provide<CustomerRepository> { CustomerRepositoryImpl(resolve()) }
    }
}

object Customers : IntIdTable("customers", "customer_id") {
    val name = varchar("name", 255)
    val email = varchar("email", 255).uniqueIndex()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
}

object Bookings : IntIdTable("bookings", "booking_id") {
    val customerId = reference("customer_id", Customers)
    val bookingDate = timestamp("booking_date").defaultExpression(CurrentTimestamp)
    val amount = double("amount")
}

interface CustomerRepository {
    suspend fun findAll(): List<CustomerWithBooking>
    suspend fun save(create: CreateCustomer): Customer
    suspend fun createBooking(id: Int, amount: Double): Booking
    suspend fun find(id: Int): CustomerWithBooking?
    suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer?
    suspend fun delete(id: Int): Boolean
}

class CustomerRepositoryImpl(private val database: R2dbcDatabase) : CustomerRepository {
    override suspend fun findAll(): List<CustomerWithBooking> = suspendTransaction(db = database) {
        Customers.selectAll()
            .map { row -> row.toCustomerWithBooking() }.toList()
    }

    override suspend fun createBooking(id: Int, amount: Double): Booking =
        suspendTransaction(db = database) {
            Bookings.insertReturning {
                it[Bookings.customerId] = id
                it[Bookings.bookingDate] = bookingDate
                it[Bookings.amount] = amount
            }.single().toBooking()
        }

    override suspend fun save(create: CreateCustomer): Customer = suspendTransaction(db = database) {
        Customers.insertReturning { insert ->
            insert[Customers.name] = name
            insert[Customers.email] = email
        }.single().toCustomer()
    }

    override suspend fun find(id: Int): CustomerWithBooking? =
        suspendTransaction(db = database) {
            Customers.selectAll()
                .where { Customers.id eq id }
                .singleOrNull()
                ?.toCustomerWithBooking()
        }

    override suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer? =
        suspendTransaction(db = database) {
            Customers.updateReturning(where = { Customers.id eq id }) { update ->
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

    private fun ResultRow.toBooking(): Booking =
        Booking(
            id = this[Bookings.id].value,
            customerId = this[Bookings.customerId].value,
            bookingDate = this[Bookings.bookingDate],
            amount = this[Bookings.amount],
        )

    private suspend fun ResultRow.toCustomerWithBooking(): CustomerWithBooking {
        val customerId = this[Customers.id].value
        val bookings = Bookings.selectAll()
            .where { Bookings.customerId eq customerId }
            .map { it.toBooking() }
            .toList()
        return CustomerWithBooking(
            id = customerId,
            name = this[Customers.name],
            email = this[Customers.email],
            createdAt = this[Customers.createdAt],
            bookings = bookings
        )
    }
}
