package xyz.morphia.mapping.validation.classrules;


import org.bson.types.ObjectId;
import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.annotations.Id;
import xyz.morphia.mapping.MappingException;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class NonStaticInnerClassTest extends TestBase {

    @Test(expected = MappingException.class)
    public void testInValidInnerClass() throws Exception {
        getMorphia().map(InValid.class);
    }

    @Test
    public void testValidInnerClass() throws Exception {
        getMorphia().map(Valid.class);
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
