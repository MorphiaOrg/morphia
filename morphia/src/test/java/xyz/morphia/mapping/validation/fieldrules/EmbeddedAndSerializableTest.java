package xyz.morphia.mapping.validation.fieldrules;


import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import xyz.morphia.TestBase;
import xyz.morphia.annotations.Embedded;
import xyz.morphia.annotations.Entity;
import xyz.morphia.annotations.Id;
import xyz.morphia.annotations.Serialized;
import xyz.morphia.mapping.validation.ConstraintViolationException;
import xyz.morphia.testutil.TestEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 */
public class EmbeddedAndSerializableTest extends TestBase {
    @Test
    public void embedded() {
        getMorphia().map(Project.class, Period.class);

        Project project = new Project();
        project.period = new Period();
        for (int x = 0; x < 100; x++) {
            project.periods.add(new Period());
        }
        getDs().save(project);

        Project project1 = getDs().find(Project.class).get();

        final List<Period> periods = project1.periods;
        for (int i = 0; i < periods.size(); i++) {
            compare(project.periods.get(i), periods.get(i));
        }

        compare(project.period, project1.period);
    }

    @Test(expected = ConstraintViolationException.class)
    public void testCheck() {
        getMorphia().map(E.class);
    }

    private void compare(final Period original, final Period loaded) {
        Assert.assertEquals(original.from, loaded.from);
        Assert.assertEquals(original.until, loaded.until);
    }

    public static class E extends TestEntity {
        @Embedded
        @Serialized
        private R r;
    }

    public static class R {
    }

    @Entity
    public static class Project {
        @Id
        private ObjectId id;
        @Embedded
        private Period period;

        @Embedded
        private List<Period> periods = new ArrayList<Period>();
    }

    @Embedded
    public static class Period implements Iterable<Date> {
        private Date from = new Date();
        private Date until = new Date();

        @Override
        public Iterator<Date> iterator() {
            return null;
        }
    }
}
