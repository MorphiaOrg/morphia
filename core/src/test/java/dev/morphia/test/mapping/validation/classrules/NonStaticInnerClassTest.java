package dev.morphia.test.mapping.validation.classrules;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MappingException;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;


public class NonStaticInnerClassTest extends TestBase {

    @Test(expectedExceptions = MappingException.class)
    public void testInValidInnerClass() {
        getMapper().map(InValid.class);
    }

    @Test
    public void testValidInnerClass() {
        getMapper().map(Valid.class);
    }

    @Entity
    private static class Valid {
        @Id
        private ObjectId id;
    }

    @Entity
    private class InValid {
        @Id
        private ObjectId id;
    }
}
