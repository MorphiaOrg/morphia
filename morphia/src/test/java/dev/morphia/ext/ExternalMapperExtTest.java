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


import org.junit.Test;
import dev.morphia.Key;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Scott Hernandez
 */
public class ExternalMapperExtTest extends TestBase {

    @Test
    public void testExternalMapping() throws Exception {
        final Mapper mapper = getMorphia().getMapper();
        final CloneMapper helper = new CloneMapper(mapper);
        helper.map(Skeleton.class, EntityWithNoAnnotations.class);
        final MappedClass mc = mapper.getMappedClass(EntityWithNoAnnotations.class);
        mc.update();
        assertNotNull(mc.getIdField());
        assertNotNull(mc.getEntityAnnotation());
        assertEquals("special", mc.getEntityAnnotation().value());

        EntityWithNoAnnotations ent = new EntityWithNoAnnotations();
        ent.id = "test";
        final Key<EntityWithNoAnnotations> k = getDs().save(ent);
        assertNotNull(k);
        ent = getDs().get(EntityWithNoAnnotations.class, "test");
        assertNotNull(ent);
        assertEquals("test", ent.id);
    }

    /**
     * The skeleton to apply from.
     *
     * @author skot
     */
    @Entity("special")
    private static class Skeleton {
        @Id
        private String id;
    }

    private static class EntityWithNoAnnotations {
        private String id;
    }

    private static class CloneMapper {
        private final Mapper mapper;

        CloneMapper(final Mapper mapper) {
            this.mapper = mapper;
        }

        void map(final Class sourceClass, final Class destClass) {
            final MappedClass destMC = mapper.getMappedClass(destClass);
            final MappedClass sourceMC = mapper.getMappedClass(sourceClass);
            //copy the class level annotations
            for (final Entry<Class<? extends Annotation>, List<Annotation>> e : sourceMC.getRelevantAnnotations().entrySet()) {
                if (e.getValue() != null && !e.getValue().isEmpty()) {
                    for (final Annotation ann : e.getValue()) {
                        destMC.addAnnotation(e.getKey(), ann);
                    }
                }
            }
            //copy the fields.
            for (final MappedField mf : sourceMC.getPersistenceFields()) {
                final Map<Class<? extends Annotation>, Annotation> annMap = mf.getAnnotations();
                final MappedField destMF = destMC.getMappedFieldByJavaField(mf.getJavaFieldName());
                if (destMF != null && annMap != null && !annMap.isEmpty()) {
                    for (final Entry<Class<? extends Annotation>, Annotation> e : annMap.entrySet()) {
                        destMF.addAnnotation(e.getKey(), e.getValue());
                    }
                }
            }

        }
    }
}

