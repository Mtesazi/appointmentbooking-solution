CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointments
    ADD CONSTRAINT appointments_no_confirmed_overlap
    EXCLUDE USING gist (
        branch_id WITH =,
        tsrange(appointment_date + start_time, appointment_date + end_time, '[)') WITH &&
    ) WHERE (status = 'CONFIRMED');
