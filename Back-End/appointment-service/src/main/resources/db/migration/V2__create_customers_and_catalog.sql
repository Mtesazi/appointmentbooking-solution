CREATE TABLE customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_customers_email (email)
);

CREATE TABLE branches (
    id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_branches_name (name)
);

CREATE TABLE services (
    id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    duration_minutes INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_services_name (name)
);

INSERT INTO branches (id, name) VALUES
    (1, 'Cape Town Central'),
    (2, 'Bellville Branch'),
    (3, 'Stellenbosch Branch'),
    (4, 'Durbanville Branch');

INSERT INTO services (id, name, duration_minutes) VALUES
    (1, 'General Consultation', 30),
    (2, 'Follow-up Consultation', 20),
    (3, 'Professional Consultation', 45),
    (4, 'Specialist Consultation', 60);

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    ADD CONSTRAINT fk_appointments_branch
        FOREIGN KEY (branch_id) REFERENCES branches (id),
    ADD CONSTRAINT fk_appointments_service
        FOREIGN KEY (service_id) REFERENCES services (id);
