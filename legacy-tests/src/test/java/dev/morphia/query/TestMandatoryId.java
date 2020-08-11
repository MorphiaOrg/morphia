package dev.morphia.query;


import dev.morphia.TestBase;
import dev.morphia.mapping.MappingException;
import org.junit.Test;

import java.util.List;


public class TestMandatoryId extends TestBase {
    @Test(expected = MappingException.class)
    public final void testMissingIdNoImplicitMapCall() {
        getMapper().map(List.of(MissingId.class));
    }
}
