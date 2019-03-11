package dev.morphia.query;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.Datastore;
import dev.morphia.TestBase;

import java.util.concurrent.atomic.AtomicInteger;

public class QueryFactoryTest extends TestBase {

    @Test
    public void changeQueryFactory() {
        final QueryFactory current = getDs().getQueryFactory();
        final QueryFactory custom = new DefaultQueryFactory();

        getDs().setQueryFactory(custom);

        Assert.assertNotSame(current, getDs().getQueryFactory());
        Assert.assertSame(custom, getDs().getQueryFactory());
    }

    @Test
    public void createQuery() {

        final AtomicInteger counter = new AtomicInteger();

        final QueryFactory queryFactory = new DefaultQueryFactory() {
            @Override
            public <T> Query<T> createQuery(final Datastore datastore, final DBCollection collection, final Class<T> type,
                                            final DBObject query) {

                counter.incrementAndGet();
                return super.createQuery(datastore, collection, type, query);
            }
        };

        getDs().setQueryFactory(queryFactory);

        final Query<String> query = getDs().find(String.class);
        final Query<String> other = getDs().find(String.class);

        Assert.assertNotSame(other, query);
        Assert.assertEquals(2, counter.get());
    }
}
