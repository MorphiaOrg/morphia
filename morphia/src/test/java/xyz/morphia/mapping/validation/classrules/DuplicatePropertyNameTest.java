package xyz.morphia.mapping.validation.classrules;


import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.annotations.Embedded;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.Property;
import xyz.morphia.mapping.validation.ConstraintViolationException;

import java.util.Map;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class DuplicatePropertyNameTest extends TestBase {
    @Test(expected = ConstraintViolationException.class)
    public void testDuplicatedPropertyNameDifferentType() throws Exception {
        getMorphia().map(DuplicatedPropertyName2.class);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testDuplicatedPropertyNameSameType() throws Exception {
        getMorphia().map(DuplicatedPropertyName.class);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testDuplicatedPropertyNameShadowedFields() throws Exception {
        getMorphia().map(Extends.class);
    }

    @Entity
    public static class DuplicatedPropertyName {
        @Id
        private String id;

        @Property(value = "value")
        private String content1;
        @Property(value = "value")
        private String content2;
    }

    @Entity
    public static class DuplicatedPropertyName2 {
        @Id
        private String id;

        @Embedded(value = "value")
        private Map<String, Integer> content1;
        @Property(value = "value")
        private String content2;
    }

    @Entity
    public static class Super {
        private String foo;
    }

    public static class Extends extends Super {
        private String foo;
    }

}
