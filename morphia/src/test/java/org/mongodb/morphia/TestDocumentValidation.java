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

import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcernException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;
import org.junit.Test;
import org.mongodb.morphia.annotations.Validation;
import org.mongodb.morphia.entities.DocumentValidation;
import org.mongodb.morphia.mapping.MappedClass;
import sun.reflect.annotation.AnnotationParser;

import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("Since15")
public class TestDocumentValidation extends TestBase {
    @Test
    public void createValidation() {
        getMorphia().map(DocumentValidation.class);
        getDs().enableDocumentValidation();
        assertEquals(Document.parse(DocumentValidation.class.getAnnotation(Validation.class).value()), getValidator());

        try {
            getDs().save(new DocumentValidation("John", 1, new Date()));
            fail("Document should have failed validation");
        } catch (WriteConcernException e) {
            assertTrue(e.getMessage().contains("Document failed validation"));
        }

        getDs().save(new DocumentValidation("Harold", 100, new Date()));

    }

    @Test
    public void validationDocuments() {
        Document validator = Document.parse("{ jelly : { $ne : 'rhubarb' } }");
        getMorphia().map(DocumentValidation.class);
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(DocumentValidation.class);

        for (ValidationLevel level : EnumSet.allOf(ValidationLevel.class)) {
            for (ValidationAction action : EnumSet.allOf(ValidationAction.class)) {
                checkValidation(validator, mappedClass, level, action);
            }
        }
    }

    private void checkValidation(final Document validator, final MappedClass mappedClass, final ValidationLevel level,
                                   final ValidationAction action) {
        updateValidation(mappedClass, level, action);
        Document expected = new Document("validator", validator)
            .append("validationLevel", level.getValue())
            .append("validationAction", action.getValue());

        assertEquals(expected, getValidation());
    }

    private  void updateValidation(final MappedClass mappedClass, final ValidationLevel level, final ValidationAction action) {
        Validation validation = createAnnotationInstance("{ jelly : { $ne : 'rhubarb' } }", level, action);
        ((DatastoreImpl) getDs()).process(mappedClass, validation);
    }

    private static Validation createAnnotationInstance(final String validator, final ValidationLevel level, final ValidationAction action) {
        Map<String, Object> values = new HashMap<String, Object>();

        values.put("value", validator);
        values.put("level", level);
        values.put("action", action);

        return (Validation) AnnotationParser.annotationForMap(Validation.class, values);
    }

    @Test
    public void overwriteValidation() {
        Document validator = Document.parse("{ jelly : { $ne : 'rhubarb' } }");
        ValidationOptions options = new ValidationOptions()
            .validator(validator)
            .validationLevel(ValidationLevel.MODERATE)
            .validationAction(ValidationAction.ERROR);
        MongoDatabase database = getMongoClient().getDatabase(TEST_DB_NAME);
        database.getCollection("validation").drop();
        database.createCollection("validation", new CreateCollectionOptions().validationOptions(options));

        assertEquals(validator, getValidator());

        Document rhubarb = new Document("jelly", "rhubarb").append("number", 20);
        database.getCollection("validation").insertOne(new Document("jelly", "grape"));
        try {
            database.getCollection("validation").insertOne(rhubarb);
            fail("Document should have failed validation");
        } catch (MongoWriteException e) {
            assertTrue(e.getMessage().contains("Document failed validation"));
        }

        getMorphia().map(DocumentValidation.class);
        getDs().enableDocumentValidation();
        assertEquals(Document.parse(DocumentValidation.class.getAnnotation(Validation.class).value()), getValidator());

        try {
            database.getCollection("validation").insertOne(rhubarb);
        } catch (MongoWriteException e) {
            assertFalse(e.getMessage().contains("Document failed validation"));
        }

        try {
            getDs().save(new DocumentValidation("John", 1, new Date()));
            fail("Document should have failed validation");
        } catch (WriteConcernException e) {
            assertTrue(e.getMessage().contains("Document failed validation"));
        }
    }

    @SuppressWarnings("unchecked")
    private Document getValidator() {
        return (Document) getValidation().get("validator");
    }

    private Document getValidation() {
        Document document = getMongoClient().getDatabase(TEST_DB_NAME)
                                            .runCommand(new Document("listCollections", 1)
                                                            .append("filter", new Document("name", "validation")));

        List<Document> firstBatch = (List<Document>) ((Document) document.get("cursor")).get("firstBatch");
        return (Document) firstBatch.get(0).get("options");
    }
}
