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

object Bookings : IntIdTable("BOOKINGS", "BOOKING_ID") {
    val customerId = reference("CUSTOMER_ID", Customers)
    val bookingDate = timestamp("BOOKING_DATE").defaultExpression(CurrentTimestamp)
    val amount = double("AMOUNT")
}

interface CustomerRepository {
    suspend fun findAll(): List<CustomerWithBooking>
    suspend fun save(create: CreateCustomer): Customer
    suspend fun createBooking(id: Int, amount: Double): Booking
    suspend fun find(id: Int): CustomerWithBooking?
    suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer?
    suspend fun delete(id: Int): Boolean
}

class CustomerRepositoryImpl(private val database: Database) : CustomerRepository {

    override suspend fun findAll(): List<CustomerWithBooking> = transaction(db = database) {
        Customers.selectAll()
            .map { row -> row.toCustomerWithBooking() }.toList()
    }

    override suspend fun createBooking(id: Int, amount: Double): Booking =
        transaction(db = database) {
            val id = Bookings.insertAndGetId {
                it[Bookings.customerId] = id
                it[Bookings.amount] = amount
            }
            Bookings.selectAll().where { Bookings.id eq id }.single().toBooking()
        }

    override suspend fun save(create: CreateCustomer): Customer = transaction(db = database) {
        val id = Customers.insertAndGetId { insert ->
            insert[Customers.name] = create.name
            insert[Customers.email] = create.email
        }
        Customers.selectAll().where { Customers.id eq id }.single().toCustomer()
    }

    override suspend fun find(id: Int): CustomerWithBooking? =
        transaction(db = database) {
            Customers.selectAll()
                .where { Customers.id eq id }
                .singleOrNull()
                ?.toCustomerWithBooking()
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

    private fun ResultRow.toBooking(): Booking =
        Booking(
            id = this[Bookings.id].value,
            customerId = this[Bookings.customerId].value,
            bookingDate = this[Bookings.bookingDate],
            amount = this[Bookings.amount],
        )

    private fun ResultRow.toCustomerWithBooking(): CustomerWithBooking {
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
