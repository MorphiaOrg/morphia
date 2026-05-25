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

package dev.morphia.test.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.City;
import dev.morphia.test.models.CityPopulation;
import dev.morphia.test.models.State;
import dev.morphia.test.models.generics.Another;
import dev.morphia.test.models.generics.Child;
import dev.morphia.test.models.generics.ChildEntity;
import dev.morphia.test.models.generics.EmbeddedType;
import dev.morphia.test.models.generics.FatherEntity;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;

public class PathTargetTest extends TestBase {

    @Test
    public void arrays() {
        withTestConfig(List.of(EntityWithListsAndArrays.class, EmbeddedType.class, Another.class, Child.class), () -> {
            Mapper mapper = getMapper();
            EntityModel entityModel = mapper.getEntityModel(EntityWithListsAndArrays.class);

            PathTarget pathTarget = new PathTarget(mapper, entityModel, "listEmbeddedType.1.anotherField");
            Assertions.assertEquals("listEmbeddedType.1.anotherField", pathTarget.translatedPath());
            Assertions.assertEquals(pathTarget.target(), mapper.getEntityModel(Another.class).getProperty("anotherField"));

            Assertions.assertEquals("listEmbeddedType.$", new PathTarget(mapper, entityModel, "listEmbeddedType.$").translatedPath());
            Assertions.assertEquals("listEmbeddedType.1", new PathTarget(mapper, entityModel, "listEmbeddedType.1").translatedPath());
        });
    }

    @Test
    public void disableValidation() {
        withTestConfig(List.of(FatherEntity.class), () -> {
            Mapper mapper = getMapper();

            final PathTarget pathTarget = new PathTarget(mapper, FatherEntity.class, "nested.field.fail", false);
            Assertions.assertEquals("nested.field.fail", pathTarget.translatedPath());
            Assertions.assertNull(pathTarget.target());
        });
    }

    @Test
    public void dottedPath() {
        withTestConfig(List.of(State.class, CityPopulation.class), () -> {
            Mapper mapper = getMapper();

            PathTarget pathTarget = new PathTarget(mapper, State.class, "biggestCity.population");
            Assertions.assertEquals("biggestCity.pop", pathTarget.translatedPath());
            Assertions.assertEquals(pathTarget.target(), mapper.getEntityModel(CityPopulation.class).getProperty("population"));
        });
    }

    @Test
    public void interfaces() {
        withTestConfig(List.of(HoldsAnInterface.class, MappedInterface.class,
                InterfaceTypeA.class, InterfaceTypeB.class), () -> {

                    Mapper mapper = getMapper();
                    EntityModel entityModel = mapper.getEntityModel(HoldsAnInterface.class);

                    PathTarget pathTarget = new PathTarget(mapper, entityModel, "mapped.value");
                    Assertions.assertEquals("mapped.value", pathTarget.translatedPath());
                    Assertions.assertEquals(pathTarget.target(), mapper.getEntityModel(InterfaceTypeB.class).getProperty("value"));

                    pathTarget = new PathTarget(mapper, entityModel, "mapped.field");
                    Assertions.assertEquals("mapped.field", pathTarget.translatedPath());
                    Assertions.assertEquals(pathTarget.target(), mapper.getEntityModel(InterfaceTypeA.class).getProperty("field"));
                });
    }

    @Test
    public void maps() {
        withTestConfig(List.of(Student.class), () -> {
            Mapper mapper = getMapper();
            EntityModel entityModel = mapper.getEntityModel(Student.class);

            PathTarget pathTarget = new PathTarget(mapper, entityModel, "grades.$.data.name");
            Assertions.assertEquals("grades.$.d.name", pathTarget.translatedPath());
            Assertions.assertEquals(pathTarget.target(), mapper.getEntityModel(Grade.class).getProperty("data"));

            pathTarget = new PathTarget(mapper, entityModel, "grades.$.d.name");
            Assertions.assertEquals("grades.$.d.name", pathTarget.translatedPath());
            Assertions.assertEquals(pathTarget.target(), mapper.getEntityModel(Grade.class).getProperty("d"));
        });
    }

    @Test
    public void propertyNameResolution() {
        withTestConfig(List.of(City.class, EmbeddedType.class), () -> {
            Mapper mapper = getMapper();
            EntityModel entityModel = mapper.getEntityModel(City.class);

            PathTarget pathTarget = new PathTarget(mapper, entityModel, "name");
            Assertions.assertEquals("city", pathTarget.translatedPath());
            Assertions.assertEquals(pathTarget.target(), entityModel.getProperty("name"));

            pathTarget = new PathTarget(mapper, entityModel, "city");
            Assertions.assertEquals("city", pathTarget.translatedPath());
            Assertions.assertEquals(pathTarget.target(), entityModel.getProperty("city"));
        });
    }

    @Test
    public void subClasses() {
        withTestConfig(List.of(FatherEntity.class, ChildEntity.class, Another.class), () -> {
            Mapper mapper = getMapper();

            PathTarget pathTarget = new PathTarget(mapper, FatherEntity.class, "embedded.anotherField");
            Assertions.assertEquals("embedded.anotherField", pathTarget.translatedPath());
            Assertions.assertEquals(pathTarget.target(), mapper.getEntityModel(Another.class).getProperty("anotherField"));
        });
    }

    @Entity
    private static class Grade {
        private int marks;

        @Property("d")
        private Map<String, String> data;

        public Grade() {
        }

        public Grade(int marks, Map<String, String> data) {
            this.marks = marks;
            this.data = data;
        }

        @Override
        public String toString() {
            return ("marks: " + marks + ", data: " + data);
        }
    }

    @Entity
    private static class Student {
        @Id
        private long id;

        private List<Grade> grades;

        public Student() {
        }

        public Student(long id, Grade... grades) {
            this.id = id;
            this.grades = asList(grades);
        }

        @Override
        public String toString() {
            return ("id: " + id + ", grades: " + grades);
        }
    }

    @Entity
    private interface MappedInterface {

    }

    @Entity
    @SuppressWarnings("unused")
    private static class EntityWithListsAndArrays {
        @Id
        private ObjectId id;
        private String[] arrayOfStrings;
        private int[] arrayOfInts;
        private List<String> listOfStrings;
        private List<Integer> listOfIntegers;
        private List<String> arrayListOfStrings;
        private List<Integer> arrayListOfIntegers;
        private List<EmbeddedType> listEmbeddedType;
        private Set<Integer> setOfIntegers;
        private String notAnArrayOrList;

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + Arrays.hashCode(arrayOfStrings);
            result = 31 * result + Arrays.hashCode(arrayOfInts);
            result = 31 * result + (listOfStrings != null ? listOfStrings.hashCode() : 0);
            result = 31 * result + (listOfIntegers != null ? listOfIntegers.hashCode() : 0);
            result = 31 * result + (arrayListOfStrings != null ? arrayListOfStrings.hashCode() : 0);
            result = 31 * result + (arrayListOfIntegers != null ? arrayListOfIntegers.hashCode() : 0);
            result = 31 * result + (listEmbeddedType != null ? listEmbeddedType.hashCode() : 0);
            result = 31 * result + (setOfIntegers != null ? setOfIntegers.hashCode() : 0);
            result = 31 * result + (notAnArrayOrList != null ? notAnArrayOrList.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EntityWithListsAndArrays)) {
                return false;
            }

            final EntityWithListsAndArrays that = (EntityWithListsAndArrays) o;

            if (!Objects.equals(id, that.id)) {
                return false;
            }
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            if (!Arrays.equals(arrayOfStrings, that.arrayOfStrings)) {
                return false;
            }
            if (!Arrays.equals(arrayOfInts, that.arrayOfInts)) {
                return false;
            }
            if (!Objects.equals(listOfStrings, that.listOfStrings)) {
                return false;
            }
            if (!Objects.equals(listOfIntegers, that.listOfIntegers)) {
                return false;
            }
            if (!Objects.equals(arrayListOfStrings, that.arrayListOfStrings)) {
                return false;
            }
            if (!Objects.equals(arrayListOfIntegers, that.arrayListOfIntegers)) {
                return false;
            }
            if (!Objects.equals(listEmbeddedType, that.listEmbeddedType)) {
                return false;
            }
            if (!Objects.equals(setOfIntegers, that.setOfIntegers)) {
                return false;
            }
            return Objects.equals(notAnArrayOrList, that.notAnArrayOrList);

        }
    }

    @Entity
    private static class HoldsAnInterface {
        @Id
        private ObjectId id;
        private MappedInterface mapped;
    }

    @Entity
    private static class InterfaceTypeA implements MappedInterface {
        private String field;
    }

    @Entity
    private static class InterfaceTypeB implements MappedInterface {
        private String value;
    }
}
