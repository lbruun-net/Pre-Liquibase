/*
 * Copyright 2021 lbruun.net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package net.lbruun.springboot.preliquibase.utils;

import java.sql.SQLException;
import javax.sql.DataSource;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import net.lbruun.springboot.preliquibase.PreLiquibaseException;

/**
 * Utility methods for Liquibase
 *
 * @author lbruun
 */
public class LiquibaseUtils {

  private LiquibaseUtils() {}

  // @formatter:off
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
   * <p>
   * Note that this is a fairly heavy operation as it involves a round-trip to the
   * database. As the information does not change it is best to use this method
   * only once and then cache the result.
   *
   * @param dataSource input
   * @return Liquibase database shortname, always lower case, never null;
   * @throws PreLiquibaseException.ResolveDbPlatformError on all kinds of errors
   */
  // @formatter:on
  public static String getLiquibaseDatabaseShortName(DataSource dataSource) {
    // Credit https://github.com/zorglube for having the Liquibase project add Autocloseable
    // interface to their Database and JdbcConnection classes and for proposing simplification in
    // this method.
    try (JdbcConnection jdbcConnection = new JdbcConnection(dataSource.getConnection());
        Database database =
            DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection)) {
      return database.getShortName();
    } catch (SQLException ex1) {
      throw new PreLiquibaseException.ResolveDbPlatformError(
          "Could not acquire connection for DataSource", ex1);
    } catch (DatabaseException ex2) {
      throw new PreLiquibaseException.ResolveDbPlatformError(
          "Error while finding Liquibase Database implementation for DataSource", ex2);
    } catch (Exception ex3) {
      throw new PreLiquibaseException.ResolveDbPlatformError(
          "Unexpected error while finding Liquibase Database implementation for DataSource", ex3);
    }
  }
}
