-- Dedicated sequences for human-readable IDs (studentId, staffId).
-- Separate from the entities' UUID primary keys — these exist purely to
-- hand out the next sequential number atomically, with no risk of two
-- concurrent registrations getting the same number, and no risk of
-- reusing a number after a row is deleted (unlike COUNT(*) + 1).
CREATE SEQUENCE IF NOT EXISTS student_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS staff_id_seq START WITH 1 INCREMENT BY 1;