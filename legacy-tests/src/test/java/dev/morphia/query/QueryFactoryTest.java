package dev.morphia.query;

import dev.morphia.TestBase;
import org.junit.Assert;
import org.junit.Test;

public class QueryFactoryTest extends TestBase {

    @Test
    public void changeQueryFactory() {
        final QueryFactory current = getDs().getQueryFactory();
        final QueryFactory custom = new DefaultQueryFactory();

        getDs().setQueryFactory(custom);

        Assert.assertNotSame(current, getDs().getQueryFactory());
        Assert.assertSame(custom, getDs().getQueryFactory());
    }

}
