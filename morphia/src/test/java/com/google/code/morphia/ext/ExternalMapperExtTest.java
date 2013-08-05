/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.google.code.morphia.ext;


import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import com.google.code.morphia.Key;
import com.google.code.morphia.TestBase;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.mapping.MappedClass;
import com.google.code.morphia.mapping.MappedField;
import com.google.code.morphia.mapping.Mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author Scott Hernandez
 */
public class ExternalMapperExtTest extends TestBase {

  /**
   * The skeleton to apply from.
   *
   * @author skot
   */
  @Entity("special")
  private static class Skeleton {
    @Id String id;
  }

  private static class EntityWithNoAnnotations {
    String id;
  }

  private static class CloneMapper {
    final Mapper mapper;

    public CloneMapper(final Mapper mapper) {
      this.mapper = mapper;
    }

    void map(final Class sourceClass, final Class destClass) {
      final MappedClass destMC = mapper.getMappedClass(destClass);
      final MappedClass sourceMC = mapper.getMappedClass(sourceClass);
      //copy the class level annotations
      for (final Entry<Class<? extends Annotation>, ArrayList<Annotation>> e : sourceMC.getRelevantAnnotations().entrySet()) {
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

  @Test
  public void testExternalMapping() throws Exception {
    final Mapper mapper = morphia.getMapper();
    final CloneMapper helper = new CloneMapper(mapper);
    helper.map(Skeleton.class, EntityWithNoAnnotations.class);
    final MappedClass mc = mapper.getMappedClass(EntityWithNoAnnotations.class);
    mc.update();
    assertNotNull(mc.getIdField());
    assertNotNull(mc.getEntityAnnotation());
    assertEquals("special", mc.getEntityAnnotation().value());

    EntityWithNoAnnotations ent = new EntityWithNoAnnotations();
    ent.id = "test";
    final Key<EntityWithNoAnnotations> k = ds.save(ent);
    assertNotNull(k);
    ent = ds.get(EntityWithNoAnnotations.class, "test");
    assertNotNull(ent);
    assertEquals("test", ent.id);
  }
}

