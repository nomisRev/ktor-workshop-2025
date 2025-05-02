package org.jetbrains.customers

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.invoke
import io.ktor.server.plugins.di.provide
import io.ktor.server.plugins.di.resolve
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

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
    fun findAll(): List<CustomerWithBooking>
    fun save(create: CreateCustomer): Customer
    fun createBooking(id: Int, amount: Double): Booking
    fun find(id: Int): CustomerWithBooking?
    fun update(id: Int, updateCustomer: UpdateCustomer): Customer?
    fun delete(id: Int): Boolean
}

class CustomerRepositoryImpl(private val database: Database) : CustomerRepository {
    override fun findAll(): List<CustomerWithBooking> = transaction(database) {
        CustomerDAO.all().map { it.toCustomerWithBooking() }
    }

    override fun createBooking(customerId: Int, amount: Double): Booking =
        transaction(database) {
            BookingsDAO.new {
                bookingDate = Clock.System.now()
                customer = CustomerDAO[customerId]
                this.amount = amount
            }.toBooking()
        }

    override fun save(create: CreateCustomer): Customer = transaction(database) {
        CustomerDAO.new {
            name = create.name
            email = create.email
            createdAt = Clock.System.now()
        }.toCustomer()
    }

    override fun find(id: Int): CustomerWithBooking? =
        transaction(database) {
            CustomerDAO.findById(id)?.toCustomerWithBooking()
        }

    override fun update(id: Int, updateCustomer: UpdateCustomer): Customer? =
        transaction(database) {
            CustomerDAO.findByIdAndUpdate(id) {
                if (updateCustomer.name != null) it.name = updateCustomer.name
                if (updateCustomer.email != null) it.email = updateCustomer.email
            }?.toCustomer()
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
        return CustomerWithBooking(
            id = customerId,
            name = this[Customers.name],
            email = this[Customers.email],
            createdAt = this[Customers.createdAt],
            bookings = bookings
        )
    }
}
