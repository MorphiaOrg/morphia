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


import com.mongodb.DBObject;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.EntityInterceptor;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.FindOptions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;


/**
 * @author Scott Hernandez
 */
public class NewAnnotationTest extends TestBase {

    @Test
    public void testIt() {
        MappedField.addInterestingAnnotation(Lowercase.class);
        getMorphia().getMapper().addInterceptor(new ToLowercaseHelper());
        getMorphia().map(User.class);
        final User u = new User();
        u.email = "ScottHernandez@gmail.com";

        getDs().save(u);

        final User uScott = getDs().find(User.class).disableValidation().filter("email_lowercase", u.email.toLowerCase())
                                   .find(new FindOptions().limit(1))
                                   .tryNext();
        Assert.assertNotNull(uScott);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface Lowercase {
    }

    private static class User {
        @Id
        private String id;
        @Lowercase
        private String email;
    }

    private static class ToLowercaseHelper implements EntityInterceptor {
        @Override
        public void postLoad(final Object ent, final DBObject dbObj, final Mapper mapper) {
        }

        @Override
        public void postPersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
        }

        @Override
        public void preLoad(final Object ent, final DBObject dbObj, final Mapper mapper) {
        }

        @Override
        public void prePersist(final Object ent, final DBObject dbObj, final Mapper mapper) {
            final MappedClass mc = mapper.getMappedClass(ent);
            final List<MappedField> toLowercase = mc.getFieldsAnnotatedWith(Lowercase.class);
            for (final MappedField mf : toLowercase) {
                try {
                    final Object fieldValue = mf.getFieldValue(ent);
                    dbObj.put(mf.getNameToStore() + "_lowercase", fieldValue.toString().toLowerCase());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void preSave(final Object ent, final DBObject dbObj, final Mapper mapper) {
        }
    }
}
