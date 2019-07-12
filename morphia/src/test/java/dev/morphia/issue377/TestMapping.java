package dev.morphia.issue377;

import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Serialized;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import org.bson.Document;
import org.junit.Test;

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
        User user = new User();
        user.id = 1;
        user.userObject = new SerializableObject();

        // when
        Document document = getMapper().toDocument(user);
        User object = getMapper().fromDocument(User.class, document);

        // then
        assertThat(object.userObject, is(user.userObject));
    }

    @Test
    public void testToMongoObjectCorrectlyMapsSerializableFieldForIssue591() {
        // given
        User user = new User();
        user.id = 1;
        user.userObject = new SerializableObject();

        MappedClass mc = getMappedClass(User.class);
        MappedField mf = mc.getMappedField("userObject");

        // when
        Object dbValue = getMapper().toMongoObject(mf, null, user.userObject);
        Class<byte[]> byteArrayClass = byte[].class;

        // then
        assertThat(dbValue, is(instanceOf(byteArrayClass)));
    }


    @Test
    public void testToMongoObjectCorrectlyMapsSerializableListOfObjectsForIssue591() {
        ListEntity user = new ListEntity();
        user.id = 1;
        List<Object> list = new ArrayList<>();
        list.add("value");
        user.list = list;

        MappedClass mc = getMappedClass(ListEntity.class);
        MappedField mf = mc.getMappedField("list");

        // when
        Object dbValue = getMapper().toMongoObject(mf, null, user.list);
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
