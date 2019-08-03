package dev.morphia.query;


import dev.morphia.mapping.MappingException;
import org.junit.Test;
import dev.morphia.Key;
import dev.morphia.TestBase;
import dev.morphia.TestMapping.MissingId;


public class TestMandatoryId extends TestBase {
    @Test(expected = MappingException.class)
    public final void testMissingIdNoImplicitMapCall() {
        final Key<MissingId> save = getDs().save(new MissingId());

        getDs().getByKey(MissingId.class, save);
    }
}
