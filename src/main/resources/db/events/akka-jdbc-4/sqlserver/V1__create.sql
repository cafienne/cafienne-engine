DROP TABLE IF EXISTS journal;

CREATE TABLE journal (
	"ordering" BIGINT IDENTITY(1,1) NOT NULL,
	"deleted" BIT NULL DEFAULT 0,
	"persistence_id" NVARCHAR(255) NOT NULL,
	"sequence_number" NUMERIC(10,0) NOT NULL,
	"tags" NVARCHAR(255) NULL DEFAULT NULL,
	"message" VARBINARY(max) NOT NULL,
	PRIMARY KEY ("persistence_id", "sequence_number")
);

CREATE UNIQUE INDEX journal_ordering_idx ON journal(ordering);

DROP TABLE IF EXISTS snapshot;

CREATE TABLE snapshot (
  "persistence_id" NVARCHAR(255) NOT NULL,
  "sequence_number" NUMERIC(10,0) NOT NULL,
  "created" NUMERIC NOT NULL,
  "snapshot" VARBINARY(max) NOT NULL,
  PRIMARY KEY ("persistence_id", "sequence_number")
);
