package xyz.morphia.query;


import org.junit.Test;
import xyz.morphia.Key;
import xyz.morphia.TestBase;
import xyz.morphia.TestMapping.MissingId;


public class TestMandatoryId extends TestBase {
    @Test(expected = ValidationException.class)
    public final void testMissingIdNoImplicitMapCall() {
        final Key<MissingId> save = getDs().save(new MissingId());

        getDs().getByKey(MissingId.class, save);
    }
}
