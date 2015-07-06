package org.mongodb.morphia.mapping.validation.classrules;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.mapping.validation.ConstraintViolationException;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.Arrays;
import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class RegisterAfterUseTest extends TestBase {

    @Test(expected = ConstraintViolationException.class)
    @Ignore("https://github.com/mongodb/morphia/issues/583")
    public void testRegisterAfterUse() throws Exception {

        // this would have failed:
        //        getMorphia().map(Broken.class);

        final Broken b = new Broken();
        b.l = Arrays.asList(1, 2, 3, 4);
        getDs().save(b); // imho must not work
        Assert.fail();

        // doe not revalidate due to being used already!
        getMorphia().map(Broken.class);
        Assert.fail();
    }

    public static class Broken extends TestEntity {
        @Property("foo")
        @Embedded("bar")
        private List l;
    }
}
