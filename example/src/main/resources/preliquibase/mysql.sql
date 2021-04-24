-- PreLiquibase
--    The following SQL gets executed prior to invoking Liquibase. 
--    It only gets executed if the database is MySQL.
--
--    Note that MySQL doesn't distingiush between catalog and schema.
--    They are one and the same thing. (MySQL uses the terminology 'database'
--    for catalog).
CREATE DATABASE IF NOT EXISTS ${spring.liquibase.liquibase-schema:medusa};
