-- Necesaria para poder usar EXCLUDE USING gist con columnas "normales" (uuid)
-- combinadas con un rango (tstzrange) en el mismo índice.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER', 'BARBER', 'ADMIN')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE barber_profiles (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    bio TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL CHECK (duration_minutes > 0),
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE barber_working_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barber_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL CHECK (
        day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')
    ),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CHECK (start_time < end_time)
);
CREATE INDEX idx_working_hours_barber_day ON barber_working_hours (barber_id, day_of_week);

CREATE TABLE barber_schedule_exceptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barber_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    date DATE NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('CLOSED', 'CUSTOM_HOURS')),
    start_time TIME,
    end_time TIME,
    CHECK (
        (type = 'CLOSED' AND start_time IS NULL AND end_time IS NULL)
        OR (type = 'CUSTOM_HOURS' AND start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
    )
);
CREATE INDEX idx_schedule_exceptions_barber_date ON barber_schedule_exceptions (barber_id, date);

CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES users (id),
    barber_id UUID NOT NULL REFERENCES users (id),
    service_id UUID NOT NULL REFERENCES services (id),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    slot tstzrange GENERATED ALWAYS AS (tstzrange(start_at, end_at, '[)')) STORED,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    price_at_booking NUMERIC(10, 2) NOT NULL,
    notes TEXT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_at > start_at),
    -- El barbero no puede tener dos citas activas que se solapen.
    CONSTRAINT no_overlap_per_barber EXCLUDE USING gist (
        barber_id WITH =,
        slot WITH &&
    ) WHERE (status NOT IN ('CANCELLED', 'NO_SHOW')),
    -- El mismo cliente no puede tener dos citas activas simultáneas.
    CONSTRAINT no_overlap_per_customer EXCLUDE USING gist (
        customer_id WITH =,
        slot WITH &&
    ) WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'))
);
CREATE INDEX idx_appointments_customer_start ON appointments (customer_id, start_at);
CREATE INDEX idx_appointments_barber_start ON appointments (barber_id, start_at);
