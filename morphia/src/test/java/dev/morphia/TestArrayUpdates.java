package dev.morphia;

import dev.morphia.mapping.Mapper;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

public class TestArrayUpdates extends TestBase {
    @Test
    public void testStudents() {
        getMapper().map(Student.class);
        final Datastore datastore = getDs();
        datastore.ensureIndexes();

        datastore.save(new Student(1L, new Grade(80, singletonMap("name", "Homework")),
                                   new Grade(90, singletonMap("name", "Test"))));

        Query<Student> testQuery = datastore.find(Student.class)
                                            .field("_id").equal(1L)
                                            .field("grades.data.name").equal("Test");
        Assert.assertNotNull(testQuery.execute(new FindOptions().limit(1))
                                      .tryNext());

        testQuery.update()
                 .set("grades.$.data.name", "Makeup Test")
                 .execute();

        Assert.assertNull(testQuery.execute(new FindOptions().limit(1))
                                   .tryNext());

        Assert.assertNotNull(datastore.find(Student.class)
                                      .field("_id").equal(1L)
                                      .field("grades.data.name").equal("Makeup Test")
                                      .execute(new FindOptions().limit(1))
                                      .tryNext());
    }

    @Test
    public void testUpdatesWithArrayIndexPosition() {
        getMapper().map(Student.class);
        final Datastore datastore = getDs();
        datastore.ensureIndexes();

        datastore.save(new Student(1L, new Grade(80, singletonMap("name", "Homework")),
                                   new Grade(90, singletonMap("name", "Test"))));

        Query<Student> testQuery = datastore.find(Student.class)
                                            .field("_id").equal(1L)
                                            .field("grades.data.name").equal("Test");
        Assert.assertNotNull(testQuery.execute(new FindOptions().limit(1))
                                      .tryNext());

        // Update the second element. Array indexes are zero-based.
        testQuery.update()
                 .set("grades.1.data.name", "Makeup Test")
                 .execute();

        Assert.assertNull(testQuery.execute(new FindOptions().limit(1))
                                   .tryNext());

        Assert.assertNotNull(datastore.find(Student.class)
                                      .field("_id").equal(1L)
                                      .field("grades.data.name").equal("Makeup Test")
                                      .execute(new FindOptions().limit(1))
                                      .tryNext());
    }

    @Test
    public void testUpdates() {
        BatchData theBatch = new BatchData();
        theBatch.files.add(new Files(0, "fileName1", "fileHash1"));
        theBatch.files.add(new Files(0, "fileName2", "fileHash2"));
        getDs().save(theBatch);
        ObjectId id = theBatch.id;

        theBatch = new BatchData();
        theBatch.files.add(new Files(0, "fileName3", "fileHash3"));
        theBatch.files.add(new Files(0, "fileName4", "fileHash4"));
        getDs().save(theBatch);
        ObjectId id2 = theBatch.id;

        getDs().find(BatchData.class)
               .filter("_id", id)
               .filter("files.fileName", "fileName1")
               .update()
               .set("files.$.fileHash", "new hash")
               .execute();


        BatchData data = getDs().find(BatchData.class)
                                .filter("_id", id)
                                .execute(new FindOptions().limit(1))
                                .tryNext();

        Assert.assertEquals("new hash", data.files.get(0).fileHash);
        Assert.assertEquals("fileHash2", data.files.get(1).fileHash);

        data = getDs().find(BatchData.class)
                      .filter("_id", id2)
                      .execute(new FindOptions().limit(1))
                      .tryNext();

        Assert.assertEquals("fileHash3", data.files.get(0).fileHash);
        Assert.assertEquals("fileHash4", data.files.get(1).fileHash);
    }

    @Entity
    public static class BatchData {

        @Id
        private ObjectId id;
        private List<Files> files = new ArrayList<Files>();

        @Override
        public String toString() {
            return format("BatchData{id=%s, files=%s}", id, files);
        }
    }

    @Embedded
    public static class Files {
        private int position;
        private String fileName = "";
        private String fileHash = "";

        public Files() {
        }

        public Files(final int pos, final String fileName, final String fileHash) {
            this.position = pos;
            this.fileName = fileName;
            this.fileHash = fileHash;
        }

        @Override
        public String toString() {
            return format("Files{fileHash='%s', fileName='%s', position=%d}", fileHash, fileName, position);
        }
    }


    @Entity
    public static class Student {
        @Id
        private long id;

        private List<Grade> grades;

        public Student() {
        }

        public Student(final long id, final Grade... grades) {
            this.id = id;
            this.grades = asList(grades);
        }

        @Override
        public String toString() {
            return ("id: " + id + ", grades: " + grades);
        }
    }

    @Embedded
    public static class Grade {
        private int marks;

        @Property("d")
        private Map<String, String> data;

        public Grade() {
        }

        public Grade(final int marks, final Map<String, String> data) {
            this.marks = marks;
            this.data = data;
        }

        @Override
        public String toString() {
            return ("marks: " + marks + ", data: " + data);
        }
    }

}
