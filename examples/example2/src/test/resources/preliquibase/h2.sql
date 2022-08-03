-- PreLiquibase
--    The following SQL gets executed prior to invoking Liquibase. 
--    It only gets executed if the database is H2.
--
CREATE SCHEMA IF NOT EXISTS ${medusa.db1-schema-name:medusa_db1};
