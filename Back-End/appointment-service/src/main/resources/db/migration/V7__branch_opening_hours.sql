CREATE TABLE branch_opening_hours (
    branch_id BIGINT NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    open_time TIME,
    close_time TIME,
    PRIMARY KEY (branch_id, day_of_week),
    CHECK ((open_time IS NULL AND close_time IS NULL)
        OR (open_time IS NOT NULL AND close_time IS NOT NULL AND open_time < close_time))
);

INSERT INTO branch_opening_hours (branch_id, day_of_week, open_time, close_time)
SELECT b.id, d.day, TIME '09:00', TIME '17:00'
FROM branches b CROSS JOIN generate_series(1, 7) AS d(day);

CREATE FUNCTION initialize_branch_opening_hours() RETURNS trigger AS $$
BEGIN
    INSERT INTO branch_opening_hours (branch_id, day_of_week, open_time, close_time)
    SELECT NEW.id, day, TIME '09:00', TIME '17:00' FROM generate_series(1, 7) AS day;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER branch_default_opening_hours
AFTER INSERT ON branches FOR EACH ROW EXECUTE FUNCTION initialize_branch_opening_hours();
