package org.mongodb.morphia.mapping.validation.classrules;


import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.testutil.TestEntity;

import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class RegisterAfterUseTest extends TestBase {

    public static class Broken extends TestEntity {
        @Property("foo")
        @Embedded("bar")
        private List l;
    }

    @Test
    @Ignore(value = "not yet implemented")
    public void testRegisterAfterUse() throws Exception {

        // this would have failed: morphia.map(Broken.class);

        final Broken b = new Broken();
        getDs().save(b); // imho must not work
        Assert.fail();

        // doe not revalidate due to being used already!
        getMorphia().map(Broken.class);
        Assert.fail();
    }
}
