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
import java.io.Serializable;
import java.time.Instant;
import net.lbruun.springboot.preliquibase.example.jpa.InstantToLongConverter;

/**
 * Example entity.
 *
 * <p>Note that as a matter of convention we use plural for table names ("LAST_LOGINS") but singular
 * for entities ("LastLogin"). However, it doesn't matter which convention you use as long as you
 * are consistent.
 */
@Entity
@Table(name = "LAST_LOGINS")
public class LastLogin implements Serializable {

  private static final long serialVersionUID = 25743358886485462L;

  @Id
  @Column(name = "LAST_LOGIN_USERNAME")
  private String username;

  @Column(name = "IP_ADDRESS")
  private String ipAddress;

  @Convert(converter = InstantToLongConverter.class)
  @Column(name = "EVENT_TIME_UTC")
  private Instant eventTime;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public Instant getEventTime() {
    return eventTime;
  }

  public void setEventTime(Instant eventTime) {
    this.eventTime = eventTime;
  }

  @Override
  public String toString() {
    return "LastLogin{"
        + "username="
        + username
        + ", ipAddress="
        + ipAddress
        + ", eventTime="
        + eventTime
        + '}';
  }
}
