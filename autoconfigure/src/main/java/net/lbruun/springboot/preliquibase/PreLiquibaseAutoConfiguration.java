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

import java.util.Objects;
import javax.sql.DataSource;
import liquibase.change.DatabaseChange;
import liquibase.integration.spring.SpringLiquibase;
import net.lbruun.springboot.preliquibase.PreLiquibaseAutoConfiguration.EnabledCondition;
import net.lbruun.springboot.preliquibase.PreLiquibaseAutoConfiguration.LiquibaseDataSourceCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.liquibase.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for Pre-Liquibase.
 *
 * @author lbruun
 */
@AutoConfiguration(
    after = {DataSourceAutoConfiguration.class},
    before = {LiquibaseAutoConfiguration.class})
@ConditionalOnClass({SpringLiquibase.class, DatabaseChange.class})
@Conditional({LiquibaseDataSourceCondition.class, EnabledCondition.class})
@ConditionalOnMissingBean({SpringLiquibase.class, PreLiquibase.class})
@EnableConfigurationProperties({
  DataSourceProperties.class,
  LiquibaseProperties.class,
  PreLiquibaseProperties.class
})
public class PreLiquibaseAutoConfiguration {

  Logger logger = LoggerFactory.getLogger(PreLiquibaseAutoConfiguration.class);

  /**
   * Returns provider which will tell which {@code DataSource} to use for Pre-Liquibase. This will
   * return a provider which will resolve to the same DataSource as used by Liquibase itself,
   * however an application can configure its own bean of type {@code
   * PreLiquibaseDataSourceProvider} and thereby override which DataSource to use for Pre-Liquibase.
   */
  @ConditionalOnMissingBean({PreLiquibaseDataSourceProvider.class})
  @Bean
  public PreLiquibaseDataSourceProvider preLiquibaseDataSourceProvider(
      ObjectProvider<DataSource> dataSource,
      @LiquibaseDataSource ObjectProvider<DataSource> liquibaseDataSource,
      LiquibaseConnectionDetails connectionDetails) {
    logger.debug("Instantiation of PreLiquibaseDataSourceProvider");

    return new DefaultPreLiquibaseDataSourceProvider(
        dataSource, liquibaseDataSource, connectionDetails);
  }

  /**
   * Create and executes PreLiquibase bean. The returned object is initialized, meaning {@link
   * PreLiquibase#execute() execute()} has been invoked.
   */
  @Bean
  public PreLiquibase preLiquibase(
      Environment environment,
      PreLiquibaseProperties properties,
      PreLiquibaseDataSourceProvider dataSourceProvider,
      ApplicationContext applicationContext) {
    logger.debug("Instantiation of PreLiquibase");

    PreLiquibase preLiquibase =
        new PreLiquibase(
            environment, dataSourceProvider.getDataSource(), properties, applicationContext);
    preLiquibase.execute();
    return preLiquibase;
  }

  /**
   * {@link BeanFactoryPostProcessor} used to dynamically declare that all {@code SpringLiquibase}
   * beans should depend on bean of type "PreLiquibase". This ensures that we get the Pre-Liquibase
   * beans executed <i>before</i> the standard Liquibase bean. Note that rather than declaring that
   * Pre-Liquibase must execute before Liquibase, we declare the opposite: that Liquibase must
   * execute after Pre-Liquibase.
   */
  @Configuration()
  @ConditionalOnClass(SpringLiquibase.class)
  static class LiquibaseOnPreLiquibaseDependencyPostProcessor
      extends AbstractDependsOnBeanFactoryPostProcessor {

    Logger logger = LoggerFactory.getLogger(LiquibaseOnPreLiquibaseDependencyPostProcessor.class);

    LiquibaseOnPreLiquibaseDependencyPostProcessor() {
      super(SpringLiquibase.class, PreLiquibase.class);
      logger.debug("Downstream dependencies on PreLiquibase are now configured");
    }
  }

  /**
   * Condition that says that either at least one DataSource bean must exist, or a
   * JdbcConnectionDetails bean must exist, or the user must have declared explicitly a JDBC URL for
   * use with Liquibase.
   */
  static final class LiquibaseDataSourceCondition extends AnyNestedCondition {

    LiquibaseDataSourceCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnBean(DataSource.class)
    private static final class DataSourceBeanCondition {}

    @ConditionalOnBean(JdbcConnectionDetails.class)
    private static final class JdbcConnectionDetailsCondition {}

    @ConditionalOnProperty(prefix = "spring.liquibase", name = "url", matchIfMissing = false)
    private static final class LiquibaseUrlCondition {}
  }

  /**
   * Condition that says that both of the properties
   *
   * <pre>
   * preliquibase.enabled
   * spring.liquibase.enabled
   * </pre>
   *
   * must not have a value of {@code false} or the property must be absent.
   */
  static final class EnabledCondition extends AllNestedConditions {

    EnabledCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(prefix = "preliquibase", name = "enabled", matchIfMissing = true)
    private static final class preLiquibaseEnabledCondition {}

    @ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", matchIfMissing = true)
    private static final class liquibaseEnabledCondition {}
  }

  /**
   * DataSource provider which resolves to the same DataSource as used by Liquibase execution. This
   * is determined by using Spring Boot's own classes.
   *
   * <p>Note: The exact methodology which Spring Boot uses to determine which DataSource to use for
   * Liquibase is not to be documented here as it may change over time. Suffice to say that given
   * the input in the constructor Spring Boot can determine this.
   */
  static class DefaultPreLiquibaseDataSourceProvider implements PreLiquibaseDataSourceProvider {

    private final DataSource dataSourceToUse;

    /**
     * Determine DataSource (based on input) which Liquibase itself is using.
     *
     * @param dataSource general DataSource (if any)
     * @param liquibaseDataSource designated DataSource for Liquibase (if any). This is typically a
     *     DataSource which has been annotated with {@code @LiquibaseDataSource} in order to mark it
     *     as designated for Liquibase. If this DataSource exists it will be used in preference to a
     *     DataSource in {@code dataSource} parameter.
     * @param connectionDetails
     */
    public DefaultPreLiquibaseDataSourceProvider(
        @NonNull ObjectProvider<DataSource> dataSource,
        @NonNull ObjectProvider<DataSource> liquibaseDataSource,
        @NonNull LiquibaseConnectionDetails connectionDetails) {

      // Here we try to mimic Spring Boot's own LiquibaseAutoConfiguration so that we figure out
      // which DataSource will (later) be used by LiquibaseAutoConfiguration.
      dataSourceToUse =
          getMigrationDataSource(
              liquibaseDataSource.getIfAvailable(), dataSource.getIfAvailable(), connectionDetails);

      // Sanity check
      Objects.requireNonNull(
          dataSourceToUse,
          "Unexpected: null value for DataSource returned from SpringLiquibase class");
    }

    @Override
    public DataSource getDataSource() {
      return dataSourceToUse;
    }

    // Copied from Spring Boot project
    private DataSource getMigrationDataSource(
        DataSource liquibaseDataSource,
        DataSource dataSource,
        LiquibaseConnectionDetails connectionDetails) {
      if (liquibaseDataSource != null) {
        return liquibaseDataSource;
      }
      String url = connectionDetails.getJdbcUrl();
      if (url != null) {
        DataSourceBuilder<?> builder =
            DataSourceBuilder.create().type(SimpleDriverDataSource.class);
        builder.url(url);
        applyConnectionDetails(connectionDetails, builder);
        return builder.build();
      }
      String user = connectionDetails.getUsername();
      if (user != null && dataSource != null) {
        DataSourceBuilder<?> builder =
            DataSourceBuilder.derivedFrom(dataSource).type(SimpleDriverDataSource.class);
        applyConnectionDetails(connectionDetails, builder);
        return builder.build();
      }
      Assert.state(dataSource != null, "Liquibase migration DataSource missing");
      return dataSource;
    }

    // Copied from Spring Boot project
    private void applyConnectionDetails(
        LiquibaseConnectionDetails connectionDetails, DataSourceBuilder<?> builder) {
      builder.username(connectionDetails.getUsername());
      builder.password(connectionDetails.getPassword());
      String driverClassName = connectionDetails.getDriverClassName();
      if (StringUtils.hasText(driverClassName)) {
        builder.driverClassName(driverClassName);
      }
    }
  }
}
