package org.jetbrains.customers

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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
        CustomerDAO.all().map { it.toCustomerWithBooking() }
    }

    override suspend fun createBooking(id: Int, amount: Double): Booking =
        transaction(db = database) {
            BookingsDAO.new {
                bookingDate = Clock.System.now()
                customer = CustomerDAO[id]
                this.amount = amount
            }.toBooking()
        }

    override suspend fun save(create: CreateCustomer): Customer = transaction(db = database) {
        CustomerDAO.new {
            name = create.name
            email = create.email
            createdAt = Clock.System.now()
        }.toCustomer()
    }

    override suspend fun find(id: Int): CustomerWithBooking? =
        transaction(db = database) {
            CustomerDAO.findById(id)?.toCustomerWithBooking()
        }

    override suspend fun update(id: Int, updateCustomer: UpdateCustomer): Customer? =
        transaction(db = database) {
            CustomerDAO.findByIdAndUpdate(id) {
                if (updateCustomer.name != null) it.name = updateCustomer.name
                if (updateCustomer.email != null) it.email = updateCustomer.email
            }?.toCustomer()
        }

    override suspend fun delete(id: Int): Boolean =
        transaction(db = database) { Customers.deleteWhere { Customers.id eq id } > 0 }
}
