CREATE TABLE IF NOT EXISTS bookings (
    booking_id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL,
    booking_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    amount DOUBLE PRECISION NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);