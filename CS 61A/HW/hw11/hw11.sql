CREATE TABLE parents AS
  SELECT "abraham" AS parent, "barack" AS child UNION
  SELECT "abraham"          , "clinton"         UNION
  SELECT "delano"           , "herbert"         UNION
  SELECT "fillmore"         , "abraham"         UNION
  SELECT "fillmore"         , "delano"          UNION
  SELECT "fillmore"         , "grover"          UNION
  SELECT "eisenhower"       , "fillmore";

CREATE TABLE dogs AS
  SELECT "abraham" AS name, "long" AS fur, 26 AS height UNION
  SELECT "barack"         , "short"      , 52           UNION
  SELECT "clinton"        , "long"       , 47           UNION
  SELECT "delano"         , "long"       , 46           UNION
  SELECT "eisenhower"     , "short"      , 35           UNION
  SELECT "fillmore"       , "curly"      , 32           UNION
  SELECT "grover"         , "short"      , 28           UNION
  SELECT "herbert"        , "curly"      , 31;

CREATE TABLE sizes AS
  SELECT "toy" AS size, 24 AS min, 28 AS max UNION
  SELECT "mini"       , 28       , 35        UNION
  SELECT "medium"     , 35       , 45        UNION
  SELECT "standard"   , 45       , 60;

-------------------------------------------------------------
-- PLEASE DO NOT CHANGE ANY SQL STATEMENTS ABOVE THIS LINE --
-------------------------------------------------------------

-- The size of each dog
CREATE TABLE size_of_dogs AS
  SELECT name as name, size as size FROM dogs, sizes WHERE min< height and height <= max;

-- All dogs with parents ordered by decreasing height of their parent
CREATE TABLE by_parent_height AS
  SELECT child FROM parents, dogs WHERE parent=name ORDER BY -height;

-- Filling out this helper table is optional
CREATE TABLE siblings AS
  SELECT d1.name as dog1, d2.name as dog2, d1.size as size FROM size_of_dogs as d1, size_of_dogs as d2, parents as p1, parents as p2 WHERE d1.size = d2.size and d1.name = p1.child and d2.name = p2.child and p1.parent = p2.parent and d1.name < d2.name;

-- Sentences about siblings that are the same size
CREATE TABLE sentences AS
  SELECT dog1 || ' and '||dog2 || ' are ' || size || ' siblings' from siblings;

-- Ways to stack 4 dogs to a height of at least 170, ordered by total height
CREATE TABLE stacks_helper(dogs, stack_height, last_height);

-- Add your INSERT INTOs here
INSERT INTO stacks_helper SELECT name||',', height, height from dogs;

INSERT INTO stacks_helper select s1.dogs||' '||s2.name ||',', s1.stack_height+ s2.height, s2.height from stacks_helper as s1, dogs as s2 WHERE s1.last_height < s2.height;

INSERT INTO stacks_helper select s1.dogs||' '||s2.name ||',', s1.stack_height+ s2.height, s2.height from stacks_helper as s1, dogs as s2 WHERE s1.last_height < s2.height;

INSERT INTO stacks_helper select s1.dogs||' '||s2.name, s1.stack_height+ s2.height, s2.height from stacks_helper as s1, dogs as s2 WHERE s1.last_height < s2.height;


CREATE table stacks as select dogs, stack_height from stacks_helper where stack_height > 170 ORDER BY stack_height;
