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
package net.lbruun.springboot.preliquibase;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.stream.Collectors.toMap;
import static net.lbruun.springboot.preliquibase.PreLiquibaseException.UninitializedError.DEFAULT;
import static net.lbruun.springboot.preliquibase.utils.LiquibaseUtils.getLiquibaseDatabaseShortName;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX;
import static org.springframework.beans.factory.config.PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_SUFFIX;
import static org.springframework.beans.factory.config.PlaceholderConfigurerSupport.DEFAULT_VALUE_SEPARATOR;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StreamUtils;

import net.lbruun.springboot.preliquibase.PreLiquibaseException.SqlScriptReadError;
import net.lbruun.springboot.preliquibase.PreLiquibaseException.SqlScriptRefError;
import net.lbruun.springboot.preliquibase.PreLiquibaseException.SqlScriptVarError;

/**
 * DataSource initializer for Pre-Liquibase module.
 *
 * <p>
 * This class locates and executes SQL scripts against a DataSource. It is meant
 * as a companion to Liquibase.
 * 
 * <p>
 * The SQL scripts to be executed are specified via the
 * {@link PreLiquibaseProperties#setSqlScriptReferences(java.util.List) sqlScriptReferences}
 * configuration property.
 * 
 * <p>
 * Prior to execution the SQL scripts are 'filtered through the environment'. 
 * This means that placeholders in the SQL scripts on the form {@code ${propertname}}
 * or {@code ${propertname:defaultvalue}} are replaced with the appropriate
 * value from the
 * <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/env/Environment.html">Spring Environment</a>.
 *
 * @author lbruun
 */
public class PreLiquibase {

    // Credit:
    // Originally this class was heavily "inspired" (read: copied from) the
    // org.springframework.boot.autoconfigure.jdbc.DataSourceInitializer class.
    // However, over time, the two now have much less in common.

    private static final Logger logger = getLogger(PreLiquibase.class);

    private final DataSource dataSource;
    private final PreLiquibaseProperties properties;
    private final ResourceLoader resourceLoader;
    private final Environment environment;
    private List<Resource> unfilteredResources;
    private List<Resource> filteredResources;
    private String dbPlatformCode;
    private boolean hasExecutedScripts;
    private volatile boolean hasExecuted = false;

    /**
     * Create a new instance with the {@link DataSource} to initialize and its
     * matching {@link PreLiquibaseProperties configuration}.
     *
     * @param environment    the source for placeholder substitution in SQL scripts
     * @param dataSource     the JDBC datasource to initialize
     * @param properties     configuration for the Pre-Liquibase module
     * @param resourceLoader the resource loader to use for locating (and
     *     loading) SQL scripts. Can be null. This is typically your {@code ApplicationContext}.
     *     If {@code null} then a {@link DefaultResourceLoader} is used.
     */
    public PreLiquibase(Environment environment, DataSource dataSource, PreLiquibaseProperties properties, ResourceLoader resourceLoader) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.properties = properties;
        this.resourceLoader = requireNonNullElseGet(resourceLoader, () -> new DefaultResourceLoader(null));
    }

    /**
     * Executes PreLiquibase.
     *
     * @throws PreLiquibaseException.ResolveDbPlatformError if there are errors
     *    while determining the type of database platform.
     * @throws PreLiquibaseException.SqlScriptReadError if SQL script cannot be read
     *     (as part of the variable substitution process)
     * @throws PreLiquibaseException.SqlScriptVarError if placeholder variables in 
     *  a SQL script cannot be resolved or if there are circular references in these
     *                                                                  placeholders.
     * @throws PreLiquibaseException.SqlScriptRefError if a SQL script specified 
     *      in the {@link PreLiquibaseProperties#setSqlScriptReferences(java.util.List) sqlScriptReferences}
     *      property cannot be found.
     * @throws org.springframework.jdbc.datasource.init.ScriptException (or subclass) if there
     *      are script execution errors. In case of a SQL error the {@code ScriptException}
     *      will wrap an exception of type {@link java.sql.SQLException SQLException}.
     */
    public synchronized void execute() {
        if (!hasExecuted) {
            hasExecuted = true;
            dbPlatformCode = resolveDbPlatformCode();
            unfilteredResources = getScripts(properties.getSqlScriptReferences());
            filteredResources = getFilteredResources(unfilteredResources);
            hasExecutedScripts = executeSQLScripts();
        }
    }

    /**
     * Gets the DataSource used by PreLiquibase.
     *
     * @return datasource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Gets the SQL scripts executed by PreLiquibase.
     * (these are the "raw" SQL scripts, meaning <i>before</i> substitution
     * of variables in the script)
     *
     * @return sql scripts
     * @throws PreLiquibaseException.UninitializedError if the method is
     *      invoked prior to {@link #execute()}.
     */
    public List<Resource> getUnfilteredResources() {
        if (!hasExecuted) {
            throw DEFAULT;
        }
        return unfilteredResources;
    }

    /**
     * Gets the SQL scripts executed by PreLiquibase.
     * (these are the "filtered" SQL scripts, meaning <i>after</i> substitution
     * of variables in the script)
     *
     * @return sql scripts
     * @throws PreLiquibaseException.UninitializedError if the method is
     *      invoked prior to {@link #execute()}.
     */
    public List<Resource> getFilteredResources() {
        if (!hasExecuted) {
            throw DEFAULT;
        }
        return filteredResources;
    }

    /**
     * Gets the resolved {@code dbPlatformCode} as used by PreLiquibase. This can
     * either be a value which PreLiquibase has decided based on auto-detection
     * of the database platform in use, or it can be a value which was set
     * explicitly by the user via the {@link PreLiquibaseProperties#setDbPlatformCode(java.lang.String) 
     * dbPlatformCode} property.
     *
     * @return database platform code
     * @throws PreLiquibaseException.UninitializedError if the method is
     *      invoked prior to {@link #execute()}.
     */
    public String getDbPlatformCode() {
        if (!hasExecuted) {
            throw DEFAULT;
        }
        return dbPlatformCode;
    }

    /**
     * Indicates if PreLiquibase has actually executed some SQL script(s) or not.
     * Reasons why nothing was executed are one of the following:
     * <ul>
     * <li>{@link PreLiquibaseProperties} {@code enabled} property was false.</li>
     * <li>No SQL files were found to execute.</li>
     * </ul>
     *
     * @return true if at least one SQL script was executed (successfully or with error)
     * @throws PreLiquibaseException.UninitializedError if the method is
     *      invoked prior to {@link #execute()}.
     */
    public boolean hasExecutedScripts() {
        if (!hasExecuted) {
            throw DEFAULT;
        }
        return hasExecutedScripts;
    }

    /**
     * Execute SQL sql scripts if required.
     *
     * @return {@code true} if the SQL script was attempted executed.
     * @throws ScriptionException (or subclass) if a SQL script files cannot be read or there's 
     *         a SQL execution error.
     */
    private boolean executeSQLScripts() {
        if (!properties.isEnabled()) {
            logger.debug("Initialization disabled (not running SQL script)");
            return false;
        }

        if (!filteredResources.isEmpty()) {
            runScripts();
            return true;
        }

        logger.debug("Pre-Liquibase disabled (no SQL script found)");
        return false;
    }

    /**
     * Auto-detect the current database platform
     */
    private String getDbPlatformCodeFromDataSource() throws PreLiquibaseException.ResolveDbPlatformError {
        logger.debug("Determining db platform from DataSource");
        final String dbPlatformCodeCandidate = getLiquibaseDatabaseShortName(dataSource);
        logger.debug("Determined database platform as '{}'", dbPlatformCodeCandidate);
        return dbPlatformCodeCandidate;
    }

    private String resolveDbPlatformCode() {
        return requireNonNullElseGet(properties.getDbPlatformCode(), this::getDbPlatformCodeFromDataSource);
    }

    private List<Resource> getScripts(List<Resource> resources) {
      if (nonNull(resources)) {
          resources.stream().filter(r -> !r.exists()).findFirst().ifPresent(r -> {
              throw new SqlScriptRefError(format("Resource \"%s\" is invalid or cannot be found", r));
          });
          return resources;
      }

      final Resource typedFallback = resourceLoader.getResource(format("classpath:preliquibase/%s.sql", dbPlatformCode));
      final Resource defaultFallback = resourceLoader.getResource("classpath:preliquibase/default.sql");

      return List.of(typedFallback.exists() ? typedFallback : defaultFallback);
    }

    private List<Resource> getFilteredResources(List<Resource> resources) {
        if (isNull(resources) || resources.isEmpty() || isNull(environment)) {
            return resources;
        }

        // In the following: If property 'spring.liquibase.liquibase-schema'
        // isn't available then use property 'spring.liquibase.default-schema'
        // instead. (Liquibase itself does the exact same thing)
        // This means the SQL script can use property
        // 'spring.liquibase.liquibase-schema'
        // even if this property is not set.
        final Map<String, String> defaultsMapping = Stream.of(new String[][] { { "spring.liquibase.liquibase-schema", "spring.liquibase.default-schema" }, }).collect(toMap(data -> data[0], data -> data[1]));

        final PreLiquibasePlaceholderResolver preLiquibasePlaceholderResolver = new PreLiquibasePlaceholderResolver(environment, defaultsMapping);
        final PropertyPlaceholderHelper placeholderReplacer = new PropertyPlaceholderHelper(DEFAULT_PLACEHOLDER_PREFIX, DEFAULT_PLACEHOLDER_SUFFIX, DEFAULT_VALUE_SEPARATOR, false /* error on unresolvable placeholders */);

        final List<Resource> newList = new ArrayList<>(resources.size());
        for (final Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                final String txt = StreamUtils.copyToString(in, properties.getSqlScriptEncoding());

                final String filteredTxt = placeholderReplacer.replacePlaceholders(txt,
                        preLiquibasePlaceholderResolver);
                if (!filteredTxt.equals(txt)) {
                    logger.debug(format("SQL script %s before replacement variable substitution : %s", resource, txt));
                    logger.debug(format("SQL script %s after replacement variable substitution : %s", resource,
                            filteredTxt));
                } else {
                    logger.debug(format("No replacement variables are in %s. Using the SQL script as-is.", resource));
                }
                newList.add(new StringShadowResource(filteredTxt, resource, properties.getSqlScriptEncoding()));
            } catch (final IOException ex) {
                throw new SqlScriptReadError(format("Could not read SQL script file \"%s into memory", resource), ex);
            } catch (final IllegalArgumentException ex) {
                throw new SqlScriptVarError(format("Could not replace variables in script file \"%s\"", resource), ex);
            }
        }
        return newList;
    }

    private void runScripts() throws ScriptException {
        final List<Resource> resources = filteredResources;
        if (resources.isEmpty()) {
            return;
        }

        resources.stream().forEach(resource -> logger.info("PreLiquibase: Executing SQL scripts : {}", resource.toString()));

        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.setContinueOnError(properties.isContinueOnError());
        populator.setSeparator(properties.getSeparator());
        if (nonNull(properties.getSqlScriptEncoding())) {
            populator.setSqlScriptEncoding(properties.getSqlScriptEncoding().name());
        }
        resources.forEach(resource -> populator.addScript(resource));
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    /**
     * PlaceholderResolver which can optionally use a backup property in-lieu of
     * another property if that property isn't available.
     */
    private static class PreLiquibasePlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

        private final Environment environment;
        private final Map<String, String> defaults;

        public PreLiquibasePlaceholderResolver(Environment environment, Map<String, String> defaults) {
            this.environment = environment;
            this.defaults = defaults;
        }

        @Override
        public String resolvePlaceholder(String placeholderName) {
            if (isNull(environment)) {
                return null;
            }
            String val = environment.getProperty(placeholderName);

            if (isNull(val) && nonNull(defaults) && defaults.containsKey(placeholderName)) {
                val = environment.getProperty(defaults.get(placeholderName));
            }
            return val;
        }
    }

    /**
     * Spring {@link Resource} where the content is a string.
     *
     * <p>
     * The object also wraps an {@code originalResource} which answers all
     * methods related to information (for example {@link Resource#getDescription()},
     * {@link Resource#getURL()}, etc). In short, the implementation shadows a
     * Resource which may originally have lived in the file system or on an URL,
     * but which now lives in-memory as a string.
     *
     * <p>
     * Note that this class was designed to fit a very specific use-case. It may
     * not fit yours.
     *
     * @author lbruun
     */
    static class StringShadowResource implements Resource {

        private final String content;
        private final Resource originalResource;
        private final Charset encoding;

        /**
         * Creates an instance of StringShadowResource.
         *
         * @param content          the content of the Resource.
         * @param originalResource the original resource from where
         * {@code content} originates.
         * @param encoding         used when the {@code content} is translated into an
         *                         InputStream.
         */
        public StringShadowResource(String content, Resource originalResource, Charset encoding) {
            this.content = content;
            this.originalResource = originalResource;
            this.encoding = encoding;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public URL getURL() throws IOException {
            return originalResource.getURL();
        }

        @Override
        public URI getURI() throws IOException {
            return originalResource.getURI();
        }

        @Override
        public File getFile() throws IOException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public long contentLength() throws IOException {
            return content.getBytes(encoding).length;
        }

        @Override
        public long lastModified() throws IOException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Resource createRelative(String relativePath) throws IOException {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public String getFilename() {
            return originalResource.getFilename();
        }

        @Override
        public String getDescription() {
            return originalResource.getDescription();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content.getBytes(encoding));
        }

        @Override
        public String toString() {
            return getDescription();
        }
    }

}
