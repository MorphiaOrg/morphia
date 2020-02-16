package dev.morphia.query.legacy;

import dev.morphia.TestBase;
import dev.morphia.query.LegacyQueryFactory;

public class LegacyTestBase extends TestBase {
    public LegacyTestBase() {
        getDs().setQueryFactory(new LegacyQueryFactory());
    }
}
