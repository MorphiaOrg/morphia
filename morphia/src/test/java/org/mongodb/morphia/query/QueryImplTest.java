package org.mongodb.morphia.query;

import com.mongodb.StubDBCollection;
import com.mongodb.StubDBCursor;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.entities.SimpleEntity;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class QueryImplTest extends TestBase {

    @Test
    public void testMaxTimeApplied() {
        // given
        StubDBCollection coll = new StubDBCollection(getDb());
        StubDBCursor stubDBCursor = new StubDBCursor(coll);
        coll.setDBCursor(stubDBCursor);

        QueryImpl<SimpleEntity> query = new QueryImpl<SimpleEntity>(SimpleEntity.class, coll, getDs());

        long maxTime = 123;
        TimeUnit maxTimeUnit = TimeUnit.MILLISECONDS;

        // when
        query.maxTime(maxTime, maxTimeUnit);
        query.prepareCursor();

        // then
        assertThat(stubDBCursor.getMaxTime(), is(maxTime));
        assertThat(stubDBCursor.getMaxTimeUnit(), is(maxTimeUnit));
    }

}