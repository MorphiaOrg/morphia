package dev.morphia.issue377;

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Serialized;
import dev.morphia.dao.BasicDAO;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.cache.DefaultEntityCache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for testing morphia mappings with Serialized
 */
public class TestMapping extends TestBase {

    @Test
    public void testCanMapSerializableObject() {
        // given
        Mapper mapper = new Mapper();
        User user = new User();
        user.id = 1;
        user.userObject = new SerializableObject();

        // when
        DBObject dbObject = mapper.toDBObject(user);
        User object = mapper.fromDBObject(getDs(), User.class, dbObject, new DefaultEntityCache());

        // then
        assertThat(object.userObject, is(user.userObject));
    }

    @Test
    public void testMapping() {
        final BasicDAO<User, ObjectId> messageDAO = new BasicDAO<User, ObjectId>(User.class, getDs());
        Assert.assertNotNull(messageDAO);

        Mapper mapper = new Mapper();

        User user = new User();
        user.id = 1;

        user.userObject = "just a String";
        DBObject dbObject = mapper.toDBObject(user);
        Object object = mapper.fromDBObject(getDs(), User.class, dbObject, new DefaultEntityCache());
        Assert.assertEquals(user.userObject, ((User) object).userObject);

        user.userObject = 33;
        dbObject = mapper.toDBObject(user);
        object = mapper.fromDBObject(getDs(), User.class, dbObject, new DefaultEntityCache());
        Assert.assertEquals(user.userObject, ((User) object).userObject);

        user.userObject = 33.3;
        dbObject = mapper.toDBObject(user);
        object = mapper.fromDBObject(getDs(), User.class, dbObject, new DefaultEntityCache());
        Assert.assertEquals(user.userObject, ((User) object).userObject);
    }

    @Test
    public void testToMongoObjectCorrectlyMapsSerializableFieldForIssue591() {
        // given
        Mapper mapper = new Mapper();

        User user = new User();
        user.id = 1;
        user.userObject = new SerializableObject();

        MappedClass mc = new MappedClass(User.class, mapper);
        MappedField mf = mc.getMappedField("userObject");

        // when
        Object dbValue = mapper.toMongoObject(mf, null, user.userObject);
        Class<byte[]> byteArrayClass = byte[].class;

        // then
        assertThat(dbValue, is(instanceOf(byteArrayClass)));
    }

    @Test
    public void testToMongoObjectCorrectlyMapsSerializableListOfObjectsForIssue591() {
        // given
        Mapper mapper = new Mapper();

        ListEntity user = new ListEntity();
        user.id = 1;
        List<Object> list = new ArrayList<Object>();
        list.add("value");
        user.list = list;

        MappedClass mc = new MappedClass(ListEntity.class, mapper);
        MappedField mf = mc.getMappedField("list");

        // when
        Object dbValue = mapper.toMongoObject(mf, null, user.list);
        Class<byte[]> byteArrayClass = byte[].class;

        // then
        assertThat(dbValue, is(instanceOf(byteArrayClass)));
    }

    @Entity
    @SuppressWarnings("unused")
    private static class User {
        @Id
        private Integer id;

        @Serialized
        private Object userObject;
    }

    @Entity
    @SuppressWarnings("unused")
    private static class ListEntity {
        @Id
        private Integer id;

        @Serialized
        private List<Object> list;
    }

    private static class SerializableObject implements Serializable {
        private final int someValue = 7;

        @Override
        public boolean equals(final Object o) {
            return this == o || !(o == null || getClass() != o.getClass());

        }

        @Override
        public int hashCode() {
            return someValue;
        }
    }
}
