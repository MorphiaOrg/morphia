package dev.morphia.query;

import dev.morphia.TestBase;
import dev.morphia.testmodel.User;
import org.junit.Test;

import java.util.Date;

public class Query20Test extends TestBase {
    @Test
    public void testQuery() {
        getDs().save(new User("name", new Date()));
        MorphiaQuery<User> query = new MorphiaQuery<>(User.class, getDs());

        query
            .execute();
    }

}