/*
 * Copyright 2022 lars.
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

import java.time.Instant;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * JPA Converter between Java Instant and {@code long}. This allows timestamps
 * to be stored in a database as a BIGINT while representing it in the
 * application as an {@code Instant}. Storing the value as a BIGINT avoids
 * problems with databases which does not support a true always-UTC TIMESTAMP
 * type.
 */
@Converter(autoApply = false)
public class InstantToLongConverter implements AttributeConverter<Instant, Long> {

    @Override
    public Long convertToDatabaseColumn(Instant attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.toEpochMilli();
    }

    @Override
    public Instant convertToEntityAttribute(Long dbData) {
        if (dbData == null) {
            return null;
        }
        return Instant.ofEpochMilli(dbData);
    }
}
