/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.morphia;

import com.mongodb.WriteConcernException;
import org.junit.Test;
import org.mongodb.morphia.entities.DocumentValidation;

import java.util.Date;

import static org.junit.Assert.assertTrue;

public class TestDocumentValidation extends TestBase {
    @Test
    public void createValidation() {
        getMorphia().map(DocumentValidation.class);
        getDs().enableDocumentValidation();

        try {
            getDs().save(new DocumentValidation("John", 1, new Date()));
        } catch (WriteConcernException e) {
            assertTrue(e.getMessage().contains("Document failed validation"));
        }

        getDs().save(new DocumentValidation("Harold", 100, new Date()));

    }

    @Test
    public void createValidationTwice() {
        getMorphia().map(DocumentValidation.class);
        getDs().enableDocumentValidation();
        getDs().enableDocumentValidation();
    }
}
