
DECLARE @timer_pk_name VARCHAR(255);
(SELECT @timer_pk_name = CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_NAME = 'timer'
    AND CONSTRAINT_TYPE = 'PRIMARY KEY');

EXEC('ALTER TABLE [timer] DROP CONSTRAINT ' + @timer_pk_name);

ALTER TABLE [timer] ALTER COLUMN [timer_id] varchar(255) NOT null;
ALTER TABLE [timer] ALTER COLUMN [case_instance_id] varchar(255) NOT null;
ALTER TABLE [timer] ALTER COLUMN [tenant] varchar(255) NOT null;
ALTER TABLE [timer] ALTER COLUMN [user] varchar(255) NOT null;

ALTER TABLE timer ADD PRIMARY KEY (timer_id);


DECLARE @offset_storage_pk_name NVARCHAR(255);
(SELECT @offset_storage_pk_name = CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = 'offset_storage' AND CONSTRAINT_TYPE = 'PRIMARY KEY');
EXEC('ALTER TABLE [offset_storage] DROP CONSTRAINT ' + @offset_storage_pk_name);

ALTER TABLE [offset_storage] ALTER COLUMN [name] varchar(255) NOT null;
ALTER TABLE [offset_storage] ALTER COLUMN [offset-type] varchar(255) NOT null;
ALTER TABLE [offset_storage] ALTER COLUMN [offset-value] varchar(255) NOT null;

ALTER TABLE offset_storage ADD PRIMARY KEY (name);
