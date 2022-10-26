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

import jakarta.persistence.*;
import net.lbruun.springboot.preliquibase.example.jpa.InstantToLongConverter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Example entity. Represents an "event" which happened in the application and
 * which we want to log for audit purpose.
 *
 * <p>
 * Note that as a matter of convention we use plural for table names
 * ("APP_EVENTS") but singular for entities ("AppEvent"). However, it doesn't
 * matter which convention you use as long as you are consistent.
 */
@Entity
@Table(name = "APP_EVENTS")
public class AppEvent implements Serializable {

    private static final long serialVersionUID = 3854338978648790802L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "APP_EVENT_ID")
    private long appEventId;

    @Convert(converter = InstantToLongConverter.class)
    @Column(name = "EVENT_TIME_UTC")
    private Instant eventTime;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "EVENT_TYPE")
    private EventType eventType;

    @Column(name = "EVENT_TEXT")
    private String eventText;

    public long getAppEventId() {
        return appEventId;
    }

    public void setAppEventId(long appEventId) {
        this.appEventId = appEventId;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getEventText() {
        return eventText;
    }

    public void setEventText(String eventText) {
        this.eventText = eventText;
    }

    @Override
    public String toString() {
        return "AppEvent{" + "appEventId=" + appEventId + ", eventTime=" + eventTime + ", eventType=" + eventType + ", eventText=" + eventText + '}';
    }

    public enum EventType {
        LOGIN,
        SEARCH
    }
}
