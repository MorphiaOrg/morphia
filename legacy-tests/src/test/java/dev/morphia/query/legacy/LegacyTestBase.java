package dev.morphia.query.legacy;

import dev.morphia.TestBase;
import dev.morphia.query.DefaultQueryFactory;
import dev.morphia.query.LegacyQueryFactory;
import org.junit.After;
import org.junit.Before;

public class LegacyTestBase extends TestBase {
    @After
    public void setDefaultQuery() {
        getDs().setQueryFactory(new DefaultQueryFactory());
    }

    @Before
    public void setLegacyQuery() {
        getDs().setQueryFactory(new LegacyQueryFactory());
    }
}
