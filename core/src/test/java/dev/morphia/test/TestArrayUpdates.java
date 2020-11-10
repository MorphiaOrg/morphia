package dev.morphia.test;

import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.test.models.Grade;
import dev.morphia.test.models.Student;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.lt;
import static dev.morphia.query.experimental.updates.UpdateOperators.inc;
import static dev.morphia.query.experimental.updates.UpdateOperators.set;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class TestArrayUpdates extends TestBase {
    @Test
    public void testStudents() {
        getMapper().map(Student.class);
        final Datastore datastore = getDs();
        datastore.ensureIndexes();

        datastore.save(new Student(1L, new Grade(80, singletonMap("name", "Homework")),
            new Grade(90, singletonMap("name", "Test"))));

        Query<Student> testQuery = datastore.find(Student.class)
                                            .filter(eq("_id", 1L),
                                                eq("grades.data.name", "Test"));
        assertNotNull(testQuery.iterator(new FindOptions().limit(1))
                               .tryNext());

        testQuery.update(set("grades.$.data.name", "Makeup Test"))
                 .execute();

        assertNull(testQuery.iterator(new FindOptions().limit(1))
                            .tryNext());

        assertNotNull(datastore.find(Student.class)
                               .filter(eq("_id", 1L),
                                   eq("grades.data.name", "Makeup Test")).iterator(new FindOptions().limit(1))
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
               .filter(eq("_id", id),
                   eq("files.fileName", "fileName1"))
               .update(set("files.$.fileHash", "new hash"))
               .execute();


        BatchData data = getDs().find(BatchData.class)
                                .filter(eq("_id", id)).iterator(new FindOptions().limit(1))
                                .tryNext();

        assertEquals(data.files.get(0).fileHash, "new hash");
        assertEquals(data.files.get(1).fileHash, "fileHash2");

        data = getDs().find(BatchData.class)
                      .filter(eq("_id", id2))
                      .iterator(new FindOptions().limit(1))
                      .tryNext();

        assertEquals(data.files.get(0).fileHash, "fileHash3");
        assertEquals(data.files.get(1).fileHash, "fileHash4");
    }

    @Test
    public void testUpdatesWithArrayFilters() {
        getMapper().map(Student.class, Grade.class);
        final Datastore datastore = getDs();
        datastore.ensureIndexes();

        datastore.save(new Student(1L, new Grade(80, singletonMap("name", "Homework")),
            new Grade(90, singletonMap("name", "Test"))));

        Query<Student> grade80 = datastore.find(Student.class)
                                          .filter(eq("_id", 1L),
                                              eq("grades.marks", 80));
        Query<Student> grade90 = datastore.find(Student.class)
                                          .filter(eq("_id", 1L),
                                              eq("grades.marks", 90));

        assertNotNull(grade80.iterator().tryNext());
        assertNotNull(grade90.iterator().tryNext());

        Query<Student> student = datastore.find(Student.class).filter(eq("_id", 1L));

        student.update(inc("grades.$[elem].marks", 5))
               .execute(new UpdateOptions()
                            .arrayFilter(lt("elem.marks", 90)));

        assertNull(grade80.iterator().tryNext());
        assertNotNull(grade90.iterator().tryNext());

        assertNotNull(datastore.find(Student.class)
                               .filter(eq("_id", 1L),
                                   eq("grades.marks", 85))
                               .iterator()
                               .tryNext());
        assertNotNull(grade90.iterator().tryNext());

        student.update(inc("grades.$[elem].marks", 5))
               .execute(new UpdateOptions()
                            .arrayFilter(lt("elem.marks", 90).not()));

        assertNull(grade90.iterator().tryNext());
        assertNotNull(datastore.find(Student.class)
                               .filter(eq("_id", 1L),
                                   eq("grades.marks", 95))
                               .iterator()
                               .tryNext());

    }

    @Entity
    private static class BatchData {

        private final List<Files> files = new ArrayList<>();
        @Id
        private ObjectId id;

        @Override
        public String toString() {
            return format("BatchData{id=%s, files=%s}", id, files);
        }
    }

    @Embedded
    private static class Files {
        private int position;
        private String fileName = "";
        private String fileHash = "";

        public Files() {
        }

        public Files(int pos, String fileName, String fileHash) {
            this.position = pos;
            this.fileName = fileName;
            this.fileHash = fileHash;
        }

        @Override
        public String toString() {
            return format("Files{fileHash='%s', fileName='%s', position=%d}", fileHash, fileName, position);
        }
    }

}
