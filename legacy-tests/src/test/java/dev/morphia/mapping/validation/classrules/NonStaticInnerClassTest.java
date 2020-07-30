package dev.morphia.mapping.validation.classrules;


import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MappingException;
import org.bson.types.ObjectId;
import org.junit.Test;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class NonStaticInnerClassTest extends TestBase {

    @Test(expected = MappingException.class)
    public void testInValidInnerClass() {
        getMapper().map(InValid.class);
    }

    @Test
    public void testValidInnerClass() {
        getMapper().map(Valid.class);
    }

    @Entity
    static class Valid {
        @Id
        private ObjectId id;
    }

    @Entity
    class InValid {
        @Id
        private ObjectId id;
    }
}
