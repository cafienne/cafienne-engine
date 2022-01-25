DROP TABLE IF EXISTS timer;

CREATE TABLE timer (
	"timer_id" NVARCHAR(255) NOT NULL,
	"case_instance_id" NVARCHAR(255) NOT NULL,
	"moment" [datetimeoffset](6) NOT NULL,
    "tenant" NVARCHAR(255) NOT NULL,
	"user" NVARCHAR(255) NOT NULL,
	PRIMARY KEY ("timer_id")
);

DROP TABLE IF EXISTS offset_storage;

CREATE TABLE offset_storage (
	"name" NVARCHAR(255) NOT NULL,
	"offset-type" NVARCHAR(255) NOT NULL,
	"offset-value" NVARCHAR(255) NOT NULL,
	"timestamp" [datetimeoffset](6) NOT NULL,
    PRIMARY KEY ("name")
);
