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
package net.lbruun.springboot.preliquibase.example;

import java.util.Objects;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import net.lbruun.springboot.preliquibase.PreLiquibase;
import net.lbruun.springboot.preliquibase.PreLiquibaseProperties;
import net.lbruun.springboot.preliquibase.example.jpa.JpaPropertiesEnhanced;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties({MedusaProperties.class})
public class MedusaApplicationConfig {

  /**
   * Configuration for all of the application's persistence requirements. (in our case this is two
   * distinct data sources, which we call 'db1' and 'db2')
   */
  public static class DatabaseConfiguration {

    /**
     * Creates a {@code SpringLiquibase} object based on a DataSource and LiquibaseProperties.
     *
     * <p>Adapted from
     * https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/liquibase/LiquibaseAutoConfiguration.java
     */
    public static SpringLiquibase getSpringLiquibase(
        DataSource dataSource, LiquibaseProperties liquibaseProperties) {
      SpringLiquibase liquibase = new SpringLiquibase();
      liquibase.setDataSource(dataSource);
      liquibase.setChangeLog(liquibaseProperties.getChangeLog());
      liquibase.setClearCheckSums(liquibaseProperties.isClearChecksums());
      if (!CollectionUtils.isEmpty(liquibaseProperties.getContexts())) {
        liquibase.setContexts(
            StringUtils.collectionToCommaDelimitedString(liquibaseProperties.getContexts()));
      }
      liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
      liquibase.setLiquibaseSchema(liquibaseProperties.getLiquibaseSchema());
      liquibase.setLiquibaseTablespace(liquibaseProperties.getLiquibaseTablespace());
      liquibase.setDatabaseChangeLogTable(liquibaseProperties.getDatabaseChangeLogTable());
      liquibase.setDatabaseChangeLogLockTable(liquibaseProperties.getDatabaseChangeLogLockTable());
      liquibase.setDropFirst(liquibaseProperties.isDropFirst());
      liquibase.setShouldRun(liquibaseProperties.isEnabled());
      if (!CollectionUtils.isEmpty(liquibaseProperties.getLabelFilter())) {
        liquibase.setLabelFilter(
            StringUtils.collectionToCommaDelimitedString(liquibaseProperties.getLabelFilter()));
      }
      liquibase.setChangeLogParameters(liquibaseProperties.getParameters());
      liquibase.setRollbackFile(liquibaseProperties.getRollbackFile());
      liquibase.setTestRollbackOnUpdate(liquibaseProperties.isTestRollbackOnUpdate());
      liquibase.setTag(liquibaseProperties.getTag());
      return liquibase; // Liquibase will fire as part of afterPropertiesSet() method on
      // SpringLiquibase object
    }

    // Utility methods for Spring Boot JPA setup
    private static EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(
        JpaProperties jpaProperties) {
      JpaVendorAdapter jpaVendorAdapter = createJpaVendorAdapter(jpaProperties);
      return new EntityManagerFactoryBuilder(
          jpaVendorAdapter, dataSource -> jpaProperties.getProperties(), null);
    }

    private static JpaVendorAdapter createJpaVendorAdapter(JpaProperties jpaProperties) {
      HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
      hibernateJpaVendorAdapter.getJpaPropertyMap().putAll(jpaProperties.getProperties());
      return hibernateJpaVendorAdapter;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(
        basePackageClasses = {net.lbruun.springboot.preliquibase.example.jpa.db1.Person.class},
        entityManagerFactoryRef = "db1EntityManagerFactory",
        transactionManagerRef = "db1TransactionManager")
    public static class Db1DataSourceConfiguration {

      @Bean
      @ConfigurationProperties("persistence.db1.datasource")
      public DataSourceProperties db1DataSourceProperties() {
        Class<?>[] name = new Class<?>[] {String.class};
        return new DataSourceProperties();
      }

      @Bean
      @ConfigurationProperties("persistence.db1.datasource.poolconfig")
      public DataSource db1DataSource(
          @Qualifier("db1DataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
      }

      @Bean
      @ConfigurationProperties("persistence.db1.preliquibase")
      public PreLiquibaseProperties db1PreLiquibaseProperties() {
        return new PreLiquibaseProperties();
      }

      @Bean("db1PreLiquibase")
      public PreLiquibase db1PreLiquibase(
          @Qualifier("db1DataSource") DataSource dataSource,
          Environment environment,
          @Qualifier("db1PreLiquibaseProperties") PreLiquibaseProperties properties,
          ApplicationContext applicationContext) {

        PreLiquibase preLiquibase =
            new PreLiquibase(environment, dataSource, properties, applicationContext);
        preLiquibase.execute();
        return preLiquibase;
      }

      @Bean
      @ConfigurationProperties("persistence.db1.liquibase")
      public LiquibaseProperties db1LiquibaseProperties() {
        return new LiquibaseProperties();
      }

      @Bean
      @DependsOn({"db1PreLiquibase"})
      public SpringLiquibase db1Liquibase(
          @Qualifier("db1DataSource") DataSource dataSource,
          @Qualifier("db1LiquibaseProperties") LiquibaseProperties liquibaseProperties) {
        return getSpringLiquibase(dataSource, liquibaseProperties);
      }

      @Bean
      @ConfigurationProperties("persistence.db1.jpa")
      public JpaPropertiesEnhanced db1JpaProperties() {
        return new JpaPropertiesEnhanced();
      }

      @Bean
      @DependsOn({"db1Liquibase"})
      public LocalContainerEntityManagerFactoryBean db1EntityManagerFactory(
          @Qualifier("db1DataSource") DataSource dataSource,
          @Qualifier("db1JpaProperties") JpaPropertiesEnhanced jpaProperties) {
        EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(jpaProperties);
        return builder
            .dataSource(dataSource)
            .packages(net.lbruun.springboot.preliquibase.example.jpa.db1.Person.class)
            .persistenceUnit(jpaProperties.getPersistenceUnitName())
            .build();
      }

      @Bean
      public PlatformTransactionManager db1TransactionManager(
          final @Qualifier("db1EntityManagerFactory") LocalContainerEntityManagerFactoryBean
                  entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
      }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(
        basePackageClasses = {net.lbruun.springboot.preliquibase.example.jpa.db2.AppEvent.class},
        entityManagerFactoryRef = "db2EntityManagerFactory",
        transactionManagerRef = "db2TransactionManager")
    public static class Db2DataSourceConfiguration {

      @Bean
      @ConfigurationProperties("persistence.db2.datasource")
      public DataSourceProperties db2DataSourceProperties() {
        return new DataSourceProperties();
      }

      @Bean
      @ConfigurationProperties("persistence.db2.datasource.poolconfig")
      public DataSource db2DataSource(
          @Qualifier("db2DataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
      }

      @Bean
      @ConfigurationProperties("persistence.db2.preliquibase")
      public PreLiquibaseProperties db2PreLiquibaseProperties() {
        return new PreLiquibaseProperties();
      }

      @Bean("db2PreLiquibase")
      public PreLiquibase db2PreLiquibase(
          @Qualifier("db2DataSource") DataSource dataSource,
          Environment environment,
          @Qualifier("db2PreLiquibaseProperties") PreLiquibaseProperties properties,
          ApplicationContext applicationContext) {

        PreLiquibase preLiquibase =
            new PreLiquibase(environment, dataSource, properties, applicationContext);
        preLiquibase.execute();
        return preLiquibase;
      }

      @Bean
      @ConfigurationProperties("persistence.db2.liquibase")
      public LiquibaseProperties db2LiquibaseProperties() {
        return new LiquibaseProperties();
      }

      @Bean
      @DependsOn({"db2PreLiquibase"})
      public SpringLiquibase db2Liquibase(
          @Qualifier("db2DataSource") DataSource dataSource,
          @Qualifier("db2LiquibaseProperties") LiquibaseProperties liquibaseProperties) {
        return getSpringLiquibase(dataSource, liquibaseProperties);
      }

      @Bean
      @ConfigurationProperties("persistence.db2.jpa")
      public JpaPropertiesEnhanced db2JpaProperties() {
        return new JpaPropertiesEnhanced();
      }

      @Bean
      @DependsOn({"db2Liquibase"})
      public LocalContainerEntityManagerFactoryBean db2EntityManagerFactory(
          @Qualifier("db2DataSource") DataSource dataSource,
          @Qualifier("db2JpaProperties") JpaPropertiesEnhanced jpaProperties) {
        EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(jpaProperties);
        return builder
            .dataSource(dataSource)
            .packages(net.lbruun.springboot.preliquibase.example.jpa.db2.AppEvent.class)
            .persistenceUnit(jpaProperties.getPersistenceUnitName())
            .build();
      }

      @Bean
      public PlatformTransactionManager db2TransactionManager(
          final @Qualifier("db2EntityManagerFactory") LocalContainerEntityManagerFactoryBean
                  entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
      }
    }
  }
}
