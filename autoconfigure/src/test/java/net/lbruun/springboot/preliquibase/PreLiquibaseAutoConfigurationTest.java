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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link PreLiquibaseAutoConfiguration}.
 *
 * @author lbruun
 */
@ExtendWith(OutputCaptureExtension.class)
public class PreLiquibaseAutoConfigurationTest {

    private static final String JDBC_URL1 = "jdbc:hsqldb:mem:stdtest";

    @BeforeEach
    void init(TestInfo testInfo) {
        System.out.println();
        System.out.println();
        System.out.println("Running test : " + testInfo.getDisplayName());
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                PreLiquibaseAutoConfiguration.class,
                LiquibaseAutoConfiguration.class))
            .withPropertyValues("spring.datasource.generate-unique-name=true");

    @Test
    void mainTest() {

        // This will also - indirectly - test if PreLiquibase executes before Liquibase.
        // If not, the schema 'myschema' will not have been created when Liquibase
        // executes and Liquibase will therefore fail.

        contextRunner
                .withUserConfiguration(EmbeddedDataSourceConfiguration.class)
                .withPropertyValues(
                    "spring.datasource.url=" + JDBC_URL1,
                    "sql.script.schemaname=myschema",
                    "spring.liquibase.default-schema=myschema")
                .run(assertPreLiquibase(preLiquibase -> {

                    // Assert that PreLiquibase has resolved the db platform correctly
                    assertThat(preLiquibase.getDbPlatformCode()).isEqualTo("hsqldb");

                    // Assert that something was executed.
                    assertThat(preLiquibase.hasExecutedScripts()).isTrue();

                    // Assert that only one script has executed
                    assertThat(preLiquibase.getUnfilteredResources()).hasSize(1);

                    // Assert which script was executed
                    assertThat(getScriptFileName(preLiquibase, 0)).endsWith("preliquibase/hsqldb.sql");
                }));
    }

    @Test
    void backsOffIfNotEnabledPreLiquibase() {
        contextRunner
            .withUserConfiguration(EmbeddedDataSourceConfiguration.class)
            .withPropertyValues(
                "preliquibase.enabled=false",
                "spring.datasource.url=" + JDBC_URL1)
            .run(context -> assertThat(context).doesNotHaveBean(PreLiquibase.class));
    }

    @Test
    void backsOffIfNotEnabledLiquibase() {
        contextRunner
            .withUserConfiguration(EmbeddedDataSourceConfiguration.class)
            .withPropertyValues(
                "spring.liquibase.enabled=false",
                "spring.datasource.url=" + JDBC_URL1)
            .run(context -> assertThat(context).doesNotHaveBean(PreLiquibase.class));
    }

    @Test
    void backsOffIfNoLiquibaseOnClasspath() {
        contextRunner
            .withUserConfiguration(EmbeddedDataSourceConfiguration.class)
            .withPropertyValues("spring.datasource.url=" + JDBC_URL1)
            .withClassLoader(new FilteredClassLoader("liquibase"))
            .run(context -> assertThat(context).doesNotHaveBean(PreLiquibase.class));
    }

    @Test
    void backsOffIfCustomDefinedPreLiquibase() {
        contextRunner
            .withUserConfiguration(EmbeddedDataSourceConfiguration.class, PreLiquibaseUserConfiguration.class)
            .withPropertyValues(
                "sql.script.schemaname=myschema",
                "spring.datasource.url=" + JDBC_URL1)
            .run(context -> {
                assertThat(context).hasBean("customPreLiquibase");
                assertThat(context).doesNotHaveBean("preLiquibase");
            });
    }

    @Test
    void sqlScriptsInCustomLocation() {
        contextRunner
            .withUserConfiguration(EmbeddedDataSourceConfiguration.class)
            .withPropertyValues(
                "spring.datasource.url=" + JDBC_URL1,
                "preliquibase.sql-script-references=file:src/test/resources/preliquibase-customlocation/hsqldb.sql,file:src/test/resources/preliquibase-customlocation/default.sql",
                "sql.script.schemaname=myschema",
                "spring.liquibase.default-schema=myschema")
            .run(assertPreLiquibase(preLiquibase -> {

                // Assert that PreLiquibase has resolved the db platform correctly
                assertThat(preLiquibase.getDbPlatformCode()).isEqualTo("hsqldb");

                // Assert that something was executed.
                assertThat(preLiquibase.hasExecutedScripts()).isTrue();

                // Assert that two scripts has executed
                assertThat(preLiquibase.getUnfilteredResources()).hasSize(2);

                // Assert which script was executed
                assertThat(getScriptFileName(preLiquibase, 0)).endsWith("preliquibase-customlocation/hsqldb.sql");
                assertThat(getScriptFileName(preLiquibase, 1)).endsWith("preliquibase-customlocation/default.sql");
            }));
    }

    // Utility methods

    private ContextConsumer<AssertableApplicationContext> assertPreLiquibase(Consumer<PreLiquibase> consumer) {
        return context -> {
            assertThat(context).hasSingleBean(PreLiquibase.class);
            final PreLiquibase preLiquibase = context.getBean(PreLiquibase.class);
            consumer.accept(preLiquibase);
        };
    }

    private String getScriptFileName(PreLiquibase preLiquibase, int scriptNo) {
        final List<Resource> unfilteredResources = preLiquibase.getUnfilteredResources();
        if (unfilteredResources == null || unfilteredResources.size() - 1 < scriptNo) {
            throw new RuntimeException("No such scriptNo");
        }
        final Resource res = unfilteredResources.get(scriptNo);

        try {
            return res.getURL().getPath();
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LiquibaseDataSourceConfiguration {

        @Bean
        @Primary
        DataSource normalDataSource() {
            return DataSourceBuilder.create().url("jdbc:hsqldb:mem:normal").username("sa").build();
        }

        @LiquibaseDataSource
        @Bean
        DataSource liquibaseDataSource() {
            return DataSourceBuilder.create().url("jdbc:hsqldb:mem:liquibasetest").username("sa").build();
        }

    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ PreLiquibaseProperties.class })
    static class PreLiquibaseUserConfiguration {

        @Bean
        PreLiquibase customPreLiquibase(
            Environment environment,
            PreLiquibaseProperties properties,
            ApplicationContext applicationContext) {

            return new PreLiquibase(environment, DataSourceBuilder.create().url(JDBC_URL1).build(), properties,
                    applicationContext);
        }

    }
}
