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
package net.lbruun.springboot.preliquibase.example.jpa.db2;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for 'AppEvent' entity.
 */
@Repository
public interface AppEventRepo extends JpaRepository<AppEvent, Long> {

    /**
     * Gets the 20 latest events in reverse order (latest first)
     */
    List<AppEvent> findTop20ByOrderByEventTimeDesc();
}
