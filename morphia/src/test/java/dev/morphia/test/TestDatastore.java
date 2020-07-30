package dev.morphia.test;

import com.mongodb.client.result.DeleteResult;
import dev.morphia.DeleteOptions;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.test.models.City;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.experimental.filters.Filters.gte;

class TestDatastore extends TestBase {
    @Test
    public void testDeletes() {
        DeleteResult delete = getDs().find(City.class).delete();
        Assertions.assertEquals(1, delete.getDeletedCount(), "Should only delete 1");

        City first = getDs().find(City.class).first();
        delete = getDs().delete(first);
        Assertions.assertEquals(1, delete.getDeletedCount(), "Should only delete 1");

        first = getDs().find(City.class).first();
        delete = getDs().delete(first, new DeleteOptions().multi(true));
        Assertions.assertEquals(1, delete.getDeletedCount(), "Should only delete 1");

        delete = getDs().find(City.class).delete(new DeleteOptions().multi(true));
        Assertions.assertTrue(delete.getDeletedCount() > 1, "Should the rest");
    }

    @Test
    public void testQueries() {
        MorphiaCursor<City> cursor = getDs().find(City.class)
                                            .filter(gte("population", 50_000))
                                            .iterator(new FindOptions()
                                                          .sort(Sort.ascending("name")));

        Assertions.assertTrue(cursor.hasNext());
    }

}