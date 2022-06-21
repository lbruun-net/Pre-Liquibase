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

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;

import liquibase.change.DatabaseChange;
import liquibase.integration.spring.SpringLiquibase;
import net.lbruun.springboot.preliquibase.PreLiquibaseAutoConfiguration.EnabledCondition;
import net.lbruun.springboot.preliquibase.PreLiquibaseAutoConfiguration.LiquibaseDataSourceCondition;

/**
 * Auto-configuration for Pre-Liquibase.
 *
 * @author lbruun
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ SpringLiquibase.class, DatabaseChange.class })
@Conditional({ LiquibaseDataSourceCondition.class, EnabledCondition.class })
@ConditionalOnMissingBean({ SpringLiquibase.class, PreLiquibase.class })
@AutoConfigureAfter({ DataSourceAutoConfiguration.class })
@AutoConfigureBefore({ LiquibaseAutoConfiguration.class })
@EnableConfigurationProperties({ DataSourceProperties.class, LiquibaseProperties.class, PreLiquibaseProperties.class })
public class PreLiquibaseAutoConfiguration {

    Logger logger = getLogger(PreLiquibaseAutoConfiguration.class);

    /**
     * Returns provider which will tell which {@code DataSource} to use for
     * Pre-Liquibase. This will return a provider which will resolve to the same
     * DataSource as used by Liquibase itself, however an application can
     * register its own bean of type {@code PreLiquibaseDataSourceProvider} and
     * thereby override which DataSource to use for Pre-Liquibase.
     */
    @Bean
    PreLiquibaseDataSourceProvider preLiquibaseDataSourceProvider(
        LiquibaseProperties liquibaseProperties,
        DataSourceProperties dataSourceProperties,
        ObjectProvider<DataSource> dataSource,
        @LiquibaseDataSource ObjectProvider<DataSource> liquibaseDataSource) {
        logger.debug("Instantiation of PreLiquibaseDataSourceProvider");

        return new DefaultPreLiquibaseDataSourceProvider(
            liquibaseProperties, dataSourceProperties, dataSource, liquibaseDataSource);
    }

    /**
     * Create and executes PreLiquibase bean. The returned object is initialized,
     * meaning {@link PreLiquibase#execute() execute()} has been invoked.
     */
    @Bean
    PreLiquibase preLiquibase(
        Environment environment,
        PreLiquibaseProperties properties,
        PreLiquibaseDataSourceProvider dataSourceProvider,
        ApplicationContext applicationContext) {
        logger.debug("Instantiation of PreLiquibase");

        final PreLiquibase preLiquibase = new PreLiquibase(
            environment,
            dataSourceProvider.getDataSource(),
            properties,
            applicationContext);
        preLiquibase.execute();
        return preLiquibase;
    }

    /**
     * {@link BeanFactoryPostProcessor} used to dynamically declare that all
     * {@code SpringLiquibase} beans should depend on bean of type
     * "PreLiquibase".
     * This ensures that we get the Pre-Liquibase beans executed 
     * <i>before</i> the standard Liquibase bean. 
     * Note that rather than declaring that Pre-Liquibase must execute
     * before Liquibase, we declare the opposite: that Liquibase 
     * must execute after Pre-Liquibase.
     *
     */
    @Configuration()
    @ConditionalOnClass(SpringLiquibase.class)
    static class LiquibaseOnPreLiquibaseDependencyPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

        Logger logger = getLogger(LiquibaseOnPreLiquibaseDependencyPostProcessor.class);

        LiquibaseOnPreLiquibaseDependencyPostProcessor() {
            super(SpringLiquibase.class, PreLiquibase.class);
            logger.debug("Downstream dependencies on PreLiquibase are now configured");
        }
    }

    /**
     * Condition that says that either at least one DataSource bean must exist or
     * the user must have declared explicitly a JDBC URL for use with
     * Liquibase.
     */
    static final class LiquibaseDataSourceCondition extends AnyNestedCondition {

        LiquibaseDataSourceCondition() {
            super(REGISTER_BEAN);
        }

        @ConditionalOnBean(DataSource.class)
        private static final class DataSourceBeanCondition {
        }

        @ConditionalOnProperty(prefix = "spring.liquibase", name = "url", matchIfMissing = false)
        private static final class LiquibaseUrlCondition {
        }
    }

    /**
     * Condition that says that both of the properties
     *
     *    'preliquibase.enabled'
     *    'spring.liquibase.enabled'
     *
     * must not have a value of "false"Â´or the property must be absent.
     */
    static final class EnabledCondition extends AllNestedConditions {

        EnabledCondition() {
            super(REGISTER_BEAN);
        }

        @ConditionalOnProperty(prefix = "preliquibase", name = "enabled", matchIfMissing = true)
        private static final class preLiquibaseEnabledCondition {
        }

        @ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", matchIfMissing = true)
        private static final class liquibaseEnabledCondition {
        }
    }

    /**
     * DataSource provider which resolves to the same DataSource as used by Liquibase
     * execution. This is determined by using Spring Boot's own classes.
     *
     * <p>
     * Note: The exact methodology which Spring Boot uses to determine which
     * DataSource to use for Liquibase is not to be documented here as it may
     * change over time. Suffice to say that given the input in the constructor
     * Spring Boot can determine this.
     */
    static class DefaultPreLiquibaseDataSourceProvider implements PreLiquibaseDataSourceProvider {
        private final DataSource dataSourceToUse;

        /**
         * Determine DataSource (based on input) which Liquibase itself is using.
         *
         * @param liquibaseProperties Spring Boot properties for Liquibase ("spring.liquibase")
         * @param dataSourceProperties Spring Boot properties for DataSource ("spring.datasource")
         * @param dataSource           general DataSource (if any)
         * @param liquibaseDataSource  designated DataSource for Liquibase (if any).
         *      This is typically a DataSource which has been annotated with {@code @LiquibaseDataSource}
         *      in order to mark it as designated for Liquibase. If this DataSource exists
         *      it will be used in preference to a DataSource in {@code dataSource} parameter.
         */
        public DefaultPreLiquibaseDataSourceProvider(
            @NonNull LiquibaseProperties liquibaseProperties,
            @NonNull DataSourceProperties dataSourceProperties,
            @NonNull ObjectProvider<DataSource> dataSource,
            @NonNull ObjectProvider<DataSource> liquibaseDataSource) {

            // Here we re-use Spring Boot's own LiquibaseAutoConfiguration
            // so that we figure out which DataSource will (later) be used
            // by LiquibaseAutoConfiguration. This ensures that we use the same
            // logic for figuring out which DataSource to use.

            // Note that SpringLiquibase object below gets instantiated OUTSIDE
            // of the IoC container, meaning it is just normal "new" instantiaion.
            // This is important as we do not want the SpringLiquibase's
            // afterPropertiesSet method to kick in. All we are interested in
            // is to figure out which datasource Liquibase would be using.
            final LiquibaseAutoConfiguration.LiquibaseConfiguration liquibaseConfiguration =
                new LiquibaseAutoConfiguration.LiquibaseConfiguration(liquibaseProperties);
            final SpringLiquibase liquibase =
                liquibaseConfiguration.liquibase(dataSource, liquibaseDataSource);

            // Sanity check
            dataSourceToUse = requireNonNull(liquibase.getDataSource(), "Unexpected: null value for DataSource returned from SpringLiquibase class");
        }

        @Override
        public DataSource getDataSource() {
            return dataSourceToUse;
        }
    }

}
