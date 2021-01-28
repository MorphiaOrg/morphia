package dev.morphia.test;

import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.test.models.ModelWithTimestamp;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.sql.Timestamp;

public class TestModelWithTimestamp extends TestBase {

    @Test
    public void testSaveAndFetch() {
        final long now = System.currentTimeMillis();

        final ObjectId objectId = new ObjectId();
        final ModelWithTimestamp model = new ModelWithTimestamp();

        model.setId(objectId);
        model.setTimestamp(new Timestamp(now));
        getDs().save(model);

        getDs().find(ModelWithTimestamp.class)
               .filter(Filters.eq("id", objectId))
               .first();

    }

}
