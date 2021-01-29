package dev.morphia.test;

import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.test.models.ModelWithSqlDates;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.sql.Timestamp;

public class TestModelWithSqlDates extends TestBase {

    @Test
    public void testSaveAndFetch() {
        final long now = System.currentTimeMillis();

        final ObjectId objectId = new ObjectId();
        final ModelWithSqlDates model = new ModelWithSqlDates();

        model.setId(objectId);
        model.setTimestamp(new Timestamp(now));
        model.setSqlDate(new java.sql.Date(now));
        model.setSqlTime(new java.sql.Time(now));
        getDs().save(model);

        getDs().find(ModelWithSqlDates.class)
               .filter(Filters.eq("id", objectId))
               .first();

    }

}
