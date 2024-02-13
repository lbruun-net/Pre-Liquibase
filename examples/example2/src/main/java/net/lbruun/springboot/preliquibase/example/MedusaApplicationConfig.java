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

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
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
import liquibase.integration.spring.SpringLiquibase;
import net.lbruun.springboot.preliquibase.PreLiquibase;
import net.lbruun.springboot.preliquibase.PreLiquibaseProperties;
import net.lbruun.springboot.preliquibase.example.jpa.JpaPropertiesEnchanced;

@Configuration
@EnableConfigurationProperties({MedusaProperties.class})
public class MedusaApplicationConfig {

    /**
     * Configuration for all of the application's persistence requirements. (in
     * our case this is two distinct data sources, which we call 'db1' and
     * 'db2')
     */
	public static class DatabaseConfiguration {

        /**
         * Creates a {@code SpringLiquibase} object based on a DataSource and
         * LiquibaseProperties.
         *
         * <p>
         * Adapted from
         * https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/liquibase/LiquibaseAutoConfiguration.java
         */
		public static SpringLiquibase getSpringLiquibase(
				DataSource dataSource,
				LiquibaseProperties liquibaseProperties) {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setDataSource(dataSource);
			liquibase.setChangeLog(liquibaseProperties.getChangeLog());
			liquibase.setClearCheckSums(liquibaseProperties.isClearChecksums());
			liquibase.setContexts(liquibaseProperties.getContexts());
			liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
			liquibase.setLiquibaseSchema(liquibaseProperties.getLiquibaseSchema());
			liquibase.setLiquibaseTablespace(liquibaseProperties.getLiquibaseTablespace());
			liquibase.setDatabaseChangeLogTable(liquibaseProperties.getDatabaseChangeLogTable());
			liquibase.setDatabaseChangeLogLockTable(liquibaseProperties.getDatabaseChangeLogLockTable());
			liquibase.setDropFirst(liquibaseProperties.isDropFirst());
			liquibase.setShouldRun(liquibaseProperties.isEnabled());
			liquibase.setLabelFilter(liquibaseProperties.getLabelFilter());
			liquibase.setChangeLogParameters(liquibaseProperties.getParameters());
			liquibase.setRollbackFile(liquibaseProperties.getRollbackFile());
			liquibase.setTestRollbackOnUpdate(liquibaseProperties.isTestRollbackOnUpdate());
			liquibase.setTag(liquibaseProperties.getTag());
			return liquibase; // Liquibase will fire as part of afterPropertiesSet() method on SpringLiquibase object
		}

		// Utility methods for Spring Boot JPA setup
		private static EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties jpaProperties) {
			JpaVendorAdapter jpaVendorAdapter = createJpaVendorAdapter(jpaProperties);
			return new EntityManagerFactoryBuilder(jpaVendorAdapter, jpaProperties.getProperties(), null);
		}

		private static JpaVendorAdapter createJpaVendorAdapter(JpaProperties jpaProperties) {
			HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
			hibernateJpaVendorAdapter.getJpaPropertyMap().putAll(jpaProperties.getProperties());
			return hibernateJpaVendorAdapter;
		}

		@Configuration(proxyBeanMethods = false)
		@EnableJpaRepositories(basePackageClasses = {net.lbruun.springboot.preliquibase.example.jpa.db1.Person.class},
				entityManagerFactoryRef = "db1EntityManagerFactory",
				transactionManagerRef = "db1TransactionManager")
		public static class Db1DataSourceConfiguration {

			@Bean
			@ConfigurationProperties("persistence.datasource.db1")
			DataSourceProperties db1DataSourceProperties() {
				Class<?>[] name = {String.class}; // TODO check if useful
				return new DataSourceProperties();
			}

			@Bean
			@ConfigurationProperties("persistence.datasource.poolconfig.db1")
			DataSource db1DataSource(
					@Qualifier("db1DataSourceProperties") DataSourceProperties dataSourceProperties) {
				return dataSourceProperties.initializeDataSourceBuilder().build();
			}

			@Bean
			@ConfigurationProperties("persistence.preliquibase.db1")
			PreLiquibaseProperties db1PreLiquibaseProperties() {
				return new PreLiquibaseProperties();
			}

      @Bean("db1PreLiquibase")
      PreLiquibase db1PreLiquibase(
          @Qualifier("db1DataSource") DataSource dataSource,
          Environment environment,
          @Qualifier("db1PreLiquibaseProperties") PreLiquibaseProperties properties) {

        PreLiquibase preLiquibase = new PreLiquibase(environment, dataSource, properties);
        preLiquibase.execute();
        return preLiquibase;
      }

      @Bean
      @ConfigurationProperties("persistence.liquibase.db1")
      LiquibaseProperties db1LiquibaseProperties() {
        return new LiquibaseProperties();
      }

      @Bean
      @DependsOn({"db1PreLiquibase"})
      SpringLiquibase db1Liquibase(
          @Qualifier("db1DataSource") DataSource dataSource,
          @Qualifier("db1LiquibaseProperties") LiquibaseProperties liquibaseProperties) {
        return getSpringLiquibase(dataSource, liquibaseProperties);
      }

      @Bean
      @ConfigurationProperties("persistence.jpa.db1")
      JpaPropertiesEnchanced db1JpaProperties() {
        return new JpaPropertiesEnchanced();
      }

      @Bean
      @DependsOn({"db1Liquibase"})
      LocalContainerEntityManagerFactoryBean db1EntityManagerFactory(
          @Qualifier("db1DataSource") DataSource dataSource,
          @Qualifier("db1JpaProperties") JpaPropertiesEnchanced jpaProperties) {
        EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(jpaProperties);
        return builder
            .dataSource(dataSource)
            .packages(net.lbruun.springboot.preliquibase.example.jpa.db1.Person.class)
            .persistenceUnit(jpaProperties.getPersistenceUnitName())
            .build();
      }

      @Bean
      PlatformTransactionManager db1TransactionManager(
          @Qualifier("db1EntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
      }

    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(
        basePackageClasses = {net.lbruun.springboot.preliquibase.example.jpa.db2.AppEvent.class},
        entityManagerFactoryRef = "db2EntityManagerFactory",
        transactionManagerRef = "db2TransactionManager")
    public static class Db2DataSourceConfiguration {

      @Bean
      @ConfigurationProperties("persistence.datasource.db2")
      DataSourceProperties db2DataSourceProperties() {
        return new DataSourceProperties();
      }

      @Bean
      @ConfigurationProperties("persistence.datasource.poolconfig.db2")
      DataSource db2DataSource(
          @Qualifier("db2DataSourceProperties") DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
      }

      @Bean
      @ConfigurationProperties("persistence.preliquibase.db2")
      PreLiquibaseProperties db2PreLiquibaseProperties() {
        return new PreLiquibaseProperties();
      }

      @Bean("db2PreLiquibase")
      PreLiquibase db2PreLiquibase(@Qualifier("db2DataSource") DataSource dataSource,
          Environment environment,
          @Qualifier("db2PreLiquibaseProperties") PreLiquibaseProperties properties) {

        PreLiquibase preLiquibase = new PreLiquibase(environment, dataSource, properties);
        preLiquibase.execute();
        return preLiquibase;
      }

      @Bean
      @ConfigurationProperties("persistence.liquibase.db2")
      LiquibaseProperties db2LiquibaseProperties() {
        return new LiquibaseProperties();
      }

      @Bean
      @DependsOn({"db2PreLiquibase"})
      SpringLiquibase db2Liquibase(@Qualifier("db2DataSource") DataSource dataSource,
          @Qualifier("db2LiquibaseProperties") LiquibaseProperties liquibaseProperties) {
        return getSpringLiquibase(dataSource, liquibaseProperties);
      }

      @Bean
      @ConfigurationProperties("persistence.jpa.db2")
      JpaPropertiesEnchanced db2JpaProperties() {
        return new JpaPropertiesEnchanced();
      }

      @Bean
      @DependsOn({"db2Liquibase"})
      LocalContainerEntityManagerFactoryBean db2EntityManagerFactory(
          @Qualifier("db2DataSource") DataSource dataSource,
          @Qualifier("db2JpaProperties") JpaPropertiesEnchanced jpaProperties) {
        EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(jpaProperties);
        return builder.dataSource(dataSource)
            .packages(net.lbruun.springboot.preliquibase.example.jpa.db2.AppEvent.class)
            .persistenceUnit(jpaProperties.getPersistenceUnitName()).build();
      }

      @Bean
      PlatformTransactionManager db2TransactionManager(
          @Qualifier("db2EntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
      }
    }
  }
}
