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
package net.lbruun.springboot.preliquibase.example.jpa;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Do something on the database to show that DB operations work.
 */
@Component
public class DoDbOperations implements ApplicationRunner {

    private final PersonRepo personRepo;

    public DoDbOperations(PersonRepo personRepo) {
        this.personRepo = personRepo;
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (personRepo.count() == 0) {
            Person person = new Person();
            person.setPersonId(1);
            person.setFirstName("Donald");
            person.setLastName("Duck");
            person.setBirthDate(LocalDate.of(1934, 6, 9));
            personRepo.save(person);
        }

        for (Person person : personRepo.findAll()) {
            System.out.println("Found person: " + person);
        }
    }

}
