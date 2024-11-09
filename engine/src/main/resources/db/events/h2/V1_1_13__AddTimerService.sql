CREATE TABLE IF NOT EXISTS PUBLIC."timer" (
  "timer_id" VARCHAR NOT NULL,
  "case_instance_id" VARCHAR NOT NULL,
  "moment" TIMESTAMP NOT NULL,
  "tenant" VARCHAR NOT NULL,
  "user" VARCHAR NOT NULL,
  PRIMARY KEY("timer_id")
);

DROP TABLE IF EXISTS PUBLIC."offset_storage";

CREATE TABLE PUBLIC."offset_storage" (
	"name" VARCHAR NOT NULL,
	"offset-type" VARCHAR NOT NULL,
	"offset-value" VARCHAR NOT NULL,
	"timestamp" TIMESTAMP NOT NULL,
    PRIMARY KEY ("name")
);
