DROP TABLE IF EXISTS PUBLIC."timer";

CREATE TABLE IF NOT EXISTS PUBLIC."timer" (
  "timer_id" VARCHAR(255) NOT NULL,
  "case_instance_id" VARCHAR(255) NOT NULL,
  "moment" TIMESTAMP NOT NULL,
  "tenant" VARCHAR(255) NOT NULL,
  "user" VARCHAR(255) NOT NULL,
  PRIMARY KEY("timer_id")
);

DROP TABLE IF EXISTS PUBLIC."offset_storage";

CREATE TABLE PUBLIC."offset_storage" (
	"name" VARCHAR(255) NOT NULL,
	"offset-type" VARCHAR(255) NOT NULL,
	"offset-value" VARCHAR(255) NOT NULL,
	"timestamp" TIMESTAMP NOT NULL,
    PRIMARY KEY ("name")
);
