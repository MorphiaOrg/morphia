package dev.morphia.mapping.validation.classrules;


import dev.morphia.mapping.Mapper;
import org.bson.types.ObjectId;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Id;
import dev.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class NonStaticInnerClassTest extends TestBase {

    @Test(expected = MappingException.class)
    public void testInValidInnerClass() throws Exception {
        Mapper.map(InValid.class);
    }

    @Test
    public void testValidInnerClass() throws Exception {
        Mapper.map(Valid.class);
    }

    static class Valid {
        @Id
        private ObjectId id;
    }

    class InValid {
        @Id
        private ObjectId id;
    }
}
