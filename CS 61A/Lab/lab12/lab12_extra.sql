.read lab12.sql

CREATE TABLE sp19favpets AS
  SELECT pet , count(*)from students group by pet order by -count(*) limit 10;


CREATE TABLE sp19dog AS
  SELECT pet, count(*) from students where pet= 'dog';


CREATE TABLE sp19alldogs AS
  SELECT pet, count(*) from students where pet like '%dog%';


CREATE TABLE obedienceimages AS
  SELECT seven, animal, count(*) from students where seven = '7' group by animal order by -count(*);

CREATE TABLE smallest_int_count AS
  SELECT smallest, count(*) from students group by smallest order by -smallest;
