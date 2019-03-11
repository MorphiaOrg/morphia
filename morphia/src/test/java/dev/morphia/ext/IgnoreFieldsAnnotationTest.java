/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */


package dev.morphia.ext;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.query.FindOptions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author Scott Hernandez
 */
public class IgnoreFieldsAnnotationTest extends TestBase {
    @Before
    @Override
    public void setUp() {
        super.setUp();
        MappedClass.addInterestingAnnotation(IgnoreFields.class);
        getMorphia().map(User.class);
        processIgnoreFieldsAnnotations();
    }

    @Test
    public void testIt() {
        final User u = new User();
        u.email = "ScottHernandez@gmail.com";
        u.ignored = "test";
        getDs().save(u);

        final User uLoaded = getDs().find(User.class)
                                    .find(new FindOptions().limit(1))
                                    .next();
        Assert.assertEquals("never, never", uLoaded.ignored);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @interface IgnoreFields {
        String value();
    }

    @Entity
    @IgnoreFields("ignored")
    static class User {
        @Id
        private ObjectId id;
        private String email;
        private String ignored = "never, never";
    }

    //remove any MappedField specified in @IgnoreFields on the class.
    private void processIgnoreFieldsAnnotations() {
        for (final MappedClass mc : getMorphia().getMapper().getMappedClasses()) {
            final IgnoreFields ignores = (IgnoreFields) mc.getAnnotation(IgnoreFields.class);
            if (ignores != null) {
                for (final String field : ignores.value().split(",")) {
                    final MappedField mf = mc.getMappedFieldByJavaField(field);
                    mc.getPersistenceFields().remove(mf);
                }
            }
        }
    }
}
