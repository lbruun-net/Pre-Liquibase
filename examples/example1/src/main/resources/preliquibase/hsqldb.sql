-- PreLiquibase
--    The following SQL gets executed prior to invoking Liquibase. 
--    It only gets executed if the database is HyperSQL database.
--
CREATE SCHEMA IF NOT EXISTS ${spring.liquibase.liquibase-schema:medusa};
