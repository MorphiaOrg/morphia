package org.mongodb.morphia.query;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;

import java.util.List;

public class TestTextSearching extends TestBase {
    @Override
    @Before
    public void setUp() {
        checkMinServerVersion(2.6);
        super.setUp();
    }

    @Test
    public void testTextSearch() {
        getMorphia().map(Greeting.class);
        getDs().ensureIndexes();

        getDs().save(new Greeting("good morning", "english"));
        getDs().save(new Greeting("good afternoon", "english"));
        getDs().save(new Greeting("good night", "english"));
        getDs().save(new Greeting("good riddance", "english"));

        getDs().save(new Greeting("guten Morgen", "german"));
        getDs().save(new Greeting("guten Tag", "german"));
        getDs().save(new Greeting("guten Nacht", "german"));

        getDs().save(new Greeting("buenos días", "spanish"));
        getDs().save(new Greeting("buenas tardes", "spanish"));
        getDs().save(new Greeting("buenos noches", "spanish"));

        List<Greeting> good = getDs().createQuery(Greeting.class)
                                     .search("good")
                                     .order("_id")
                                     .asList();
        Assert.assertEquals(4, good.size());
        Assert.assertEquals("good morning", good.get(0).value);
        Assert.assertEquals("good afternoon", good.get(1).value);
        Assert.assertEquals("good night", good.get(2).value);
        Assert.assertEquals("good riddance", good.get(3).value);

        good = getDs().createQuery(Greeting.class)
                      .search("good", "english")
                      .order("_id")
                      .asList();
        Assert.assertEquals(4, good.size());
        Assert.assertEquals("good morning", good.get(0).value);
        Assert.assertEquals("good afternoon", good.get(1).value);
        Assert.assertEquals("good night", good.get(2).value);
        Assert.assertEquals("good riddance", good.get(3).value);

        Assert.assertEquals(1, getDs().createQuery(Greeting.class)
                                      .search("riddance")
                                      .asList().size());
        Assert.assertEquals(1, getDs().createQuery(Greeting.class)
                                      .search("noches", "spanish")
                                      .asList().size());
        Assert.assertEquals(1, getDs().createQuery(Greeting.class)
                                      .search("Tag")
                                      .asList().size());
    }

    @Indexes(@Index(fields = @Field(value = "$**", type = IndexType.TEXT)))
    public static class Greeting {
        @Id
        private ObjectId id;
        private String value;
        private String language;

        public Greeting() {
        }

        public Greeting(final String value, final String language) {
            this.language = language;
            this.value = value;
        }
    }
}
