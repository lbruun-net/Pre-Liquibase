-- PreLiquibase
--    The following SQL gets executed prior to invoking Liquibase. 
--    It only gets executed if the database is HyperSQL database.
--
CREATE SCHEMA IF NOT EXISTS ${medusa.db2-schema-name:medusa_db2};
