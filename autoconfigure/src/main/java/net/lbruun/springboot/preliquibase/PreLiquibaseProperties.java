/*
 * Copyright 2021 lbruun.net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lbruun.springboot.preliquibase;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static net.lbruun.springboot.preliquibase.PreLiquibaseProperties.PROPERTIES_PREFIX;

/**
 * Properties for Pre-Liquibase module.
 *
 * @author lbruun
 */
@Validated
@ConfigurationProperties(prefix = PROPERTIES_PREFIX)
public class PreLiquibaseProperties {

  public static final String PROPERTIES_PREFIX = "preliquibase";
    public static final String DEFAULT_SCRIPT_LOCATION
            = "classpath:preliquibase/";

    private boolean enabled = true;

    /**
     * Database platform code to use when choosing which SQL script files
     * to execute (such as {@code preliquibase/${dbPlatformCode}.sql}).
     */
    private String dbPlatformCode;

    /**
     * SQL script resource references.
     */
  @NotEmpty(message = "sql-script-references must not be empty")
    private List<String> sqlScriptReferences = Collections.singletonList(DEFAULT_SCRIPT_LOCATION);

  /**
   * Whether to stop if an error occurs while executing the SQL script.
   */
  private boolean continueOnError = false;

  /**
   * Statement separator in SQL initialization scripts.
   */
  private String separator = ";";

    /**
     * SQL scripts encoding.
     */
    private Charset sqlScriptEncoding = StandardCharsets.UTF_8;


    /**
     * Get the 'enabled' setting (if the module is enabled or not).
     *
     * @return
     * @see #setEnabled(boolean)
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Disables or enables module. Default is {@code true} (module is enabled).
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    /**
     * Gets 'continueOnError' setting.
     *
     * @see #setContinueOnError(boolean)
     */
    public boolean isContinueOnError() {
        return this.continueOnError;
    }


  /**
   * Get the 'enabled' setting (if the module is enabled or not).
   * @see #setEnabled(boolean)
   * @return
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Disables or enables module. Default is {@code true} (module is enabled).
   * @param enabled
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }


  /**
   * Gets 'continueOnError' setting.
   * @see #setContinueOnError(boolean)
   */
  public boolean isContinueOnError() {
    return continueOnError;
  }


  /**
     * Sets whether to stop if an error occurs while executing the SQL script.
     * Default value is: {@code false}.
   */
  public void setContinueOnError(boolean continueOnError) {
    this.continueOnError = continueOnError;
  }


    /**
     * Gets 'separator' setting.
     *
     * @see #setSeparator(java.lang.String)
     */
    public String getSeparator() {
        return this.separator;
    }

    /**
     * Sets statement separator in SQL scripts.
     * Defaults to semi-colon if not set.
   */
  public void setSeparator(String separator) {
    this.separator = separator;
  }

    /**
     * Gets 'sqlScriptEncoding'.
     *
     * @return
     * @see #setSqlScriptEncoding(java.nio.charset.Charset)
     */
    public Charset getSqlScriptEncoding() {
        return this.sqlScriptEncoding;
    }

  /**
     * Sets the file encoding for SQL script file.
     * Defaults to {@code UTF-8} if not set.
     *
     * @param sqlScriptEncoding
     */
    public void setSqlScriptEncoding(Charset sqlScriptEncoding) {
        this.sqlScriptEncoding = sqlScriptEncoding;
    }

    /**
     * Gets 'dbPlatformCode'
     *
     * <p>
     * Note that this is an arbitrary value. It can be any string value.
     *
     * @return database platform code or {@code null} if the value isn't set.
     * @see #setDbPlatformCode(java.lang.String)
     */
    public String getDbPlatformCode() {
        return dbPlatformCode;
    }

    /**
     * Sets the db engine code to use when finding which SQL script to execute,
     * as in {@code preliquibase/${dbEngineCode}.sql}}.
     *
     * <p>
     * Setting this value explicitly overrides the database platform
     * auto-detection. The value can be any value; it will not be validated.
     *
     * @param dbPlatformCode
     */
    public void setDbPlatformCode(String dbPlatformCode) {
        this.dbPlatformCode = dbPlatformCode;
    }

    /**
     * Gets the sqlScriptReferences
     *
     * @return
     * @see #setSqlScriptReferences(java.util.List)
     */
    public List<String> getSqlScriptReferences() {
        return sqlScriptReferences;
    }

    /**
     * Sets location(s) of where to find the SQL script(s) to execute.
     *
     * <p>
     * The value is interpreted slightly differently depending on its
     * content:
   * <ul>
     *   <li>If the value is a Spring Resource textual reference which ends with {@code "/"}:
     *       In this case, the reference is expected to be a folder reference
     *       where SQL scripts can be found. From this folder:
     *       If a file named "{@link #setDbPlatformCode(java.lang.String)
     *       DBPLATFORMCODE}{@code .sql}" (e.g. "{@code postgresql.sql}") exists then
     *       that will be used. If no such file is found then a file named
     *       "{@code default.sql}" is used.
     *       If neither file is found then no action will be taken, similarly
     *       to {@link #setEnabled(boolean) disabling} the module.
     *       <br>
     *       Example values:<br>
     *       {@code "classpath:my-folder/"} (load from classpath folder).<br>
     *       {@code "file:c:/config/sql-scripts/"} (load from file system folder, Windows).<br>
     *       {@code "file:/app/etc/sql-scripts/"} (load from file system folder, Linux).<br>
     *       <br>
     *   </li>
     *   <li>Otherwise: The value is interpreted as a comma-separated list of
     *       Spring Resource textual references to <i>specific</i> SQL files. Each
     *       script file will be executed in the order they are listed. Before
     *       execution of any of of the script files it is checked if all files
     *       mentioned in the list actually exists. If not, an
     *       {@link PreLiquibaseException.SqlScriptRefError} exception is thrown.
     *       <br>
     *       Example value: {@code "file:/foo/bar/myscript1.sql,file:/foo/bar/myscript2.sql"}.
     *   </li>
     * </ul>
     *
     * <p>
     * Default value: {@link #DEFAULT_SCRIPT_LOCATION}.
     *
     * @param sqlScriptReferences list of Spring Resource references.
     */
    public void setSqlScriptReferences(List<String> sqlScriptReferences) {
        this.sqlScriptReferences = sqlScriptReferences;
    }
}
