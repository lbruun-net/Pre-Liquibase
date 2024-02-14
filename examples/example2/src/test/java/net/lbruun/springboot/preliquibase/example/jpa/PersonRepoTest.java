/*
 * Copyright 2021 lbruun.net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package net.lbruun.springboot.preliquibase.example.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import net.lbruun.springboot.preliquibase.example.jpa.db1.Person;
import net.lbruun.springboot.preliquibase.example.jpa.db1.PersonRepo;

/**
 * Example Unit test. Not meant to be a show case for best practice of writing Spring Boot JPA unit
 * tests.
 * <p>
 * The main point here is that you can use the @DataJpaTest annotation because it will include
 * Pre-Liquibase auto-configuration
 */
@DataJpaTest(properties = {
    "spring.liquibase.change_log=classpath:/liquibase/db1/changelog/db.changelog-master.yaml",
    "preliquibase.sql-script-references=classpath:preliquibase/db1/"})
public class PersonRepoTest {

  @Autowired
  private PersonRepo personRepo;

  @Test
  void injectedComponentsAreNotNull() {
    assertThat(personRepo).isNotNull();
  }

  @Test
  void saveAndRetrievePerson() {

    Person person = new Person();
    person.setFirstName("John");
    person.setLastName("Doe");
    person.setBirthDate(LocalDate.of(1996, 12, 4));
    personRepo.save(person);

    // See if we can find the person we just saved
    Optional<Person> personOpt = personRepo.findById(person.getPersonId());
    assertThat(personOpt).isNotEmpty();
  }
}
