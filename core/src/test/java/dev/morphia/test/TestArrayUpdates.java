package dev.morphia.test;

import dev.morphia.Datastore;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import dev.morphia.test.models.Grade;
import dev.morphia.test.models.Student;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.filters.Filters.lt;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static java.util.Collections.singletonMap;

public class TestArrayUpdates extends TestBase {

    @Test
    public void testUpdatesWithArrayFilters() {
        final Datastore datastore = getDs();

        datastore.save(new Student(1L, new Grade(80, singletonMap("name", "Homework")),
                new Grade(90, singletonMap("name", "Test"))));

        Query<Student> grade80 = datastore.find(Student.class)
                .filter(eq("_id", 1L),
                        eq("grades.marks", 80));
        Query<Student> grade90 = datastore.find(Student.class)
                .filter(eq("_id", 1L),
                        eq("grades.marks", 90));

        Assertions.assertNotNull(grade80.iterator().tryNext());
        Assertions.assertNotNull(grade90.iterator().tryNext());

        Query<Student> student = datastore.find(Student.class).filter(eq("_id", 1L));

        student.update(new UpdateOptions()
                .arrayFilter(lt("elem.marks", 90)),
                inc("grades.$[elem].marks", 5));

        Assertions.assertNull(grade80.iterator().tryNext());
        Assertions.assertNotNull(grade90.iterator().tryNext());

        Assertions.assertNotNull(datastore.find(Student.class)
                .filter(eq("_id", 1L),
                        eq("grades.marks", 85))
                .iterator()
                .tryNext());
        Assertions.assertNotNull(grade90.iterator().tryNext());

        student.update(new UpdateOptions()
                .arrayFilter(lt("elem.marks", 90).not()),
                inc("grades.$[elem].marks", 5));

        Assertions.assertNull(grade90.iterator().tryNext());
        Assertions.assertNotNull(datastore.find(Student.class)
                .filter(eq("_id", 1L),
                        eq("grades.marks", 95))
                .iterator()
                .tryNext());

    }
}
