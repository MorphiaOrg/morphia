package dev.morphia.issueA;


import dev.morphia.TestBase;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Id;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.io.Serializable;


/**
 * Test from email to mongodb-users list.
 */
public class TestMapping extends TestBase {

    @Test
    public void testMapping() {
        getMorphia().map(ClassLevelThree.class);
        final ClassLevelThree sp = new ClassLevelThree();

        //Old way
        final Document wrapObj = getMorphia().toDocument(sp);  //the error points here from the user
        getDs().getDatabase().getCollection("testColl").insertOne(wrapObj);


        //better way
        getDs().save(sp);

    }

    private interface InterfaceOne<K> {
        K getK();
    }

    private static class ClassLevelOne<K> implements InterfaceOne<K>, Serializable {
        private K k;

        @Override
        public K getK() {
            return k;
        }
    }

    private static class ClassLevelTwo extends ClassLevelOne<String> {

    }

    private static class ClassLevelThree {
        @Id
        private ObjectId id;

        private String name;

        @Embedded
        private ClassLevelTwo value;
    }

}
