USE skill_exam;
ALTER TABLE room_arrangement ADD COLUMN exam_date DATE NULL;
UPDATE room_arrangement ra
  JOIN registration reg ON ra.registration_id = reg.id
  JOIN exam_plan ep ON reg.plan_id = ep.id
SET ra.exam_date = DATE(ep.exam_time);
SELECT COUNT(*) AS null_dates FROM room_arrangement WHERE exam_date IS NULL;
ALTER TABLE room_arrangement MODIFY exam_date DATE NOT NULL;
ALTER TABLE room_arrangement DROP INDEX uk_room_seat;
CREATE UNIQUE INDEX uk_room_seat_date ON room_arrangement(room_id, seat_no, exam_date);
SHOW CREATE TABLE room_arrangement;
