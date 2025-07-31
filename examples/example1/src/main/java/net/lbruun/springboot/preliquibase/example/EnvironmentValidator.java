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

import java.text.MessageFormat;
import java.util.Locale;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Validates Spring Environment properties <i>before</i> Spring Boot auto-configuration kicks in.
 *
 * <p>The purpose here is to make sure the "medusa.envName" property is not of mixed case as it
 * would wreak havoc in the way Liquibase works.
 *
 * <p>The processor must be registered in {@code META-INF/spring.factories}.
 *
 * @author lbruun
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnvironmentValidator implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    validateMedusaEnvName(environment);
  }

  private void validateMedusaEnvName(ConfigurableEnvironment environment) {
    String envNamePropName = "medusa.envName";
    String envNameVal = environment.getProperty(envNamePropName);
    if (envNameVal != null && isMixedCase(envNameVal)) {
      String msgFormat =
          "{0} has a value, ''{1}'', which is if mixed-case. This is not allowed. Aborting.";
      if (isOsEnvVarDefined(environment, envNamePropName)) {
        throw new IllegalStateException(
            MessageFormat.format(msgFormat, "OS environment variable MEDUSA_ENVNAME", envNameVal));
      }
      if (isSysPropDefined(environment, envNamePropName)) {
        throw new IllegalStateException(
            MessageFormat.format(msgFormat, "System property " + envNamePropName, envNameVal));
      }
      throw new IllegalStateException(
          MessageFormat.format(msgFormat, "Property " + envNamePropName, envNameVal));
    }
  }

  private boolean isMixedCase(String value) {
    return !(value.toLowerCase(Locale.US).equals(value)
        || value.toUpperCase(Locale.US).equals(value));
  }

  private boolean isOsEnvVarDefined(ConfigurableEnvironment environment, String propertyName) {
    PropertySource<?> propertySource =
        environment
            .getPropertySources()
            .get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    return (propertySource != null && propertySource.containsProperty(propertyName));
  }

  private boolean isSysPropDefined(ConfigurableEnvironment environment, String propertyName) {
    PropertySource<?> propertySource =
        environment
            .getPropertySources()
            .get(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    return (propertySource != null && propertySource.containsProperty(propertyName));
  }
}
