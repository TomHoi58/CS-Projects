.read sp19data.sql

CREATE TABLE obedience AS
  SELECT seven, animal from students;

CREATE TABLE smallest_int AS
  SELECT time, smallest from students where smallest > 2 order by smallest limit 20;

CREATE TABLE matchmaker AS
  SELECT s1.pet, s1.song, s1.color, s2.color FROM students as s1, students as s2 where s1.pet = s2.pet and s1.song =s2.song and s1.time < s2.time;
