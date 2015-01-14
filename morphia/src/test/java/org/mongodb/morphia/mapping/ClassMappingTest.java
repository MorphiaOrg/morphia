package org.mongodb.morphia.mapping;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 *
 */
public class ClassMappingTest extends TestBase {

    public static class E {
        @Id
        private ObjectId id;

        @Property
        private Class<? extends Collection> testClass;
        private Class<? extends Collection> testClass2;
    }

    public static class EntityWithInterestingAnnotation {

        static {
            MappedField.addInterestingAnnotation(InterestingAnnotation.class);
        }

        @Id
        private ObjectId id;

        @Property
        private String doNotFindMe;

        @InterestingAnnotation
        @Property
        private String findMe;

    }

    public static class ExtendingEntityWithInterestingAnnotation extends EntityWithInterestingAnnotation {

        @Property
        private String extendedDoNotFindMe;

        @InterestingAnnotation
        @Property
        private String extendedFindMe;

    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface InterestingAnnotation {

    }

    @Test
    public void testMapping() throws Exception {
        E e = new E();

        e.testClass = LinkedList.class;
        getDs().save(e);

        e = getDs().get(e);
        Assert.assertEquals(LinkedList.class, e.testClass);
    }

    @Test
    public void testMappingWithoutAnnotation() throws Exception {
        E e = new E();

        e.testClass2 = LinkedList.class;
        getDs().save(e);

        e = getDs().get(e);
        Assert.assertEquals(LinkedList.class, e.testClass2);
    }

    @Test
    public void shouldFindInterestingAnnotation() throws ClassNotFoundException {
        // GIVEN
        getMorphia().map(EntityWithInterestingAnnotation.class);

        // WHEN
        EntityWithInterestingAnnotation entityWithInterestingAnnotation = new EntityWithInterestingAnnotation();
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(entityWithInterestingAnnotation);
        List<MappedField> mappedFields = mappedClass.getFieldsAnnotatedWith(InterestingAnnotation.class);

        // THEN
        Assert.assertEquals(1, mappedFields.size());
    }

    @Test
    public void shouldFindInterestingAnnotationOnExtendingClass() throws ClassNotFoundException {
        // GIVEN
        getMorphia().map(ExtendingEntityWithInterestingAnnotation.class);

        // WHEN
        EntityWithInterestingAnnotation entityWithInterestingAnnotation = new ExtendingEntityWithInterestingAnnotation();
        MappedClass mappedClass = getMorphia().getMapper().getMappedClass(entityWithInterestingAnnotation);
        List<MappedField> mappedFields = mappedClass.getFieldsAnnotatedWith(InterestingAnnotation.class);

        // THEN
        Assert.assertEquals(2, mappedFields.size());
    }
}
