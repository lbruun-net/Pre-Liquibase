/*
 * Copyright 2021 lbruun.net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lbruun.springboot.preliquibase.utils;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import net.lbruun.springboot.preliquibase.PreLiquibaseException;
import net.lbruun.springboot.preliquibase.PreLiquibaseException.ResolveDbPlatformError;

/**
 * Utility methods for Liquibase
 *
 * @author lbruun
 */
public class LiquibaseUtils {

    private LiquibaseUtils() {
    }

    /**
     * Finds the Liquibase database {@code shortname} for a DataSource.
     *
     * <p>
     * Non-complete list of possible return values:
     * <ul>
     *   <li>{@code postgresql}. PostgreSQL
     *   <li>{@code mysql}. MySQL</li>
     *   <li>{@code mariadb}. MariaDB</li>
     *   <li>{@code mssql}. Microsoft SQL Server</li>
     *   <li>{@code h2}. H2 database</li>
     *   <li>{@code hsqldb}. HyperSQL database</li>
     *   <li>{@code oracle}. Oracle Database</li>
     *   <li>{@code db2}. IBM Db2 on Linux, Unix and Windows</li>
     *   <li>{@code db2z}. IBM Db2 on zOS</li>
     *   <li>{@code derby}. Apache Derby</li>
     *   <li>{@code sqlite}. SQLite</li>
     *   <li>{@code sybase}. Sybase Adaptive Server Enterprise</li>
     *   <li>{@code unsupported}. Database not supported by Liquibase</li>
     * </ul>
     * (These are the same values as those which can be used in a Liquibase
     * {@code dbms} Precondition. Refer to Liquibase documentation for further
     * information on possible values.)
     *
     * <p>
     * The method invokes {@code liquibase.database.DatabaseFactory#findCorrectDatabaseImplementation()}
     * and therefore requires Liquibase library on the classpath.
     *
     * <p>
     * The method works like this: The database is determined by connecting to
     * it and retrieving {@link java.sql.DatabaseMetaData} which is then matched
     * against known database platforms in Liquibase. If there's any error
     * during this operation then {@link PreLiquibaseException} will be thrown.
     * If there are no errors, but the type of database simply isn't supported
     * by Liquibase then {@code "unsupported"} is returned. If all goes well a
     * Liquibase database shortname is returned as per the list above. The
     * connection used to determine the database type is closed again before the
     * method exits.
     * 
     * @param dataSource input
     * @return Liquibase database shortname, always lower case, never null;
     * @throws PreLiquibaseException.ResolveDbPlatformError on all kinds of errors
     */
    public static String getLiquibaseDatabaseShortName(DataSource dataSource) {
        // See SpringLiquibase.getDatabaseProductName() method from where
        // this code is pretty much copied.
        try (Connection connection = dataSource.getConnection();
                Database database = DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection));) {
            return database.getShortName();
        } catch (final SQLException e) {
            throw new ResolveDbPlatformError("Could not acquire connection for DataSource", e);
        } catch (final DatabaseException e) {
            throw new ResolveDbPlatformError("Error while finding Liquibase Database implementation for DataSource", e);
        } catch (final Exception e) {
            throw new ResolveDbPlatformError("Unexpected error while finding Liquibase Database implementation for DataSource", e);
        }
    }

}
