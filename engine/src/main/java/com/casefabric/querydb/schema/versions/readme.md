# Explanation of history schema traits

The traits in this package contain schema description of older CaseFabric versions that are no longer in use.
Each time a new table is e.g. removed, it can be added to a new trait with the next version number.
The trait with the previous version number then extends the new trait, such that the older versions of the schema
still contain the information of the next version. This is typically containing the table that is removed. This
table existed in the older version, and therefore must remain available for the flyway code of that older version.
Additionally the corresponding QueryDB_1_1_x schema version implements the old trait.