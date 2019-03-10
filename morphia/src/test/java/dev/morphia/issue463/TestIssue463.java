package dev.morphia.issue463;


import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;


public class TestIssue463 extends TestBase {
    @Test
    public void save() {
        getMorphia().map(Class1.class, Class2.class);

        final Class2 class2 = new Class2();
        class2.setId(new ObjectId());
        class2.setText("hello world");
        getDs().save(class2);

        final BasicDBObject query = new BasicDBObject("_id", class2.getId());
        Assert.assertFalse(getDs().getCollection(Class1.class).find(query).hasNext());
        Assert.assertTrue(getDs().getCollection(Class2.class).find(query).hasNext());
    }

    @Entity(value = "class1", noClassnameStored = true)
    public static class Class1 {
        @Id
        private ObjectId id;
        private String text;

        public ObjectId getId() {
            return id;
        }

        public void setId(final ObjectId id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }
    }

    @Entity(value = "class2", noClassnameStored = true)
    public static class Class2 extends Class1 {

    }
}
