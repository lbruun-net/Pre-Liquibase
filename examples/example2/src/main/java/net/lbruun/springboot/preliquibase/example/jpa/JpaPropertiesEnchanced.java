/*
 * Copyright 2022 lbruun.net
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
package net.lbruun.springboot.preliquibase.example.jpa;

import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;

/**
 * Enhanced version of {@link JpaProperties} which also allows to define
 * <i>Persistence Unit name</i> as a property.
 */
public class JpaPropertiesEnchanced extends JpaProperties {

    private String persistenceUnitName;

    /**
     * Get Persistence Unit name,
     *
     * @see #setPersistenceUnitName(java.lang.String)
     * @return name
     */
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    /**
     * Set name to use for the Persistence Unit. In an application with more
     * than one Persistence Unit it is beneficial to be able to tell them apart
     * by their name. The name is often used in log output.
     *
     * @param persistenceUnitName name
     */
    public void setPersistenceUnitName(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }
}
