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
package net.lbruun.springboot.preliquibase.example.jpa.db2;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Logging of applications "events" for audit purpose.
 */
@Service
public class LogService {

    private final AppEventRepo appEventRepo;
    private final LastLoginRepo lastLoginRepo;

    public LogService(AppEventRepo appEventRepo, LastLoginRepo lastLoginRepo) {
        this.appEventRepo = appEventRepo;
        this.lastLoginRepo = lastLoginRepo;
    }

    @Transactional(transactionManager = "db2TransactionManager")
    public void logLoginEvent(String username, String ipAddress) {
        Instant now = Instant.now();
        Optional<LastLogin> optLastLogin = lastLoginRepo.findById(username);
        if (optLastLogin.isPresent()) {
            LastLogin lastLogin = optLastLogin.get();
            lastLogin.setEventTime(now);
            lastLogin.setIpAddress(ipAddress);
        } else {
            LastLogin lastLogin = new LastLogin();
            lastLogin.setUsername(username);
            lastLogin.setEventTime(now);
            lastLogin.setIpAddress(ipAddress);
            lastLoginRepo.save(lastLogin);
        }

        AppEvent appEvent = new AppEvent();
        appEvent.setEventTime(now);
        appEvent.setEventType(AppEvent.EventType.LOGIN);
        appEvent.setEventText("User " + username + " has logged in from " + ipAddress);
        appEventRepo.save(appEvent);
    }

    public void logSearchEvent(String username, String searchTerget) {
        AppEvent appEvent = new AppEvent();
        appEvent.setEventTime(Instant.now());
        appEvent.setEventType(AppEvent.EventType.SEARCH);
        appEvent.setEventText("User " + username + " searched for " + searchTerget);
        appEventRepo.save(appEvent);
    }

    public List<AppEvent> getLatest20Events() {
        return appEventRepo.findTop20ByOrderByEventTimeDesc();
    }
}
