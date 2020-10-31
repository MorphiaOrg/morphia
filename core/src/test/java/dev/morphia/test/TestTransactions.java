package dev.morphia.test;

import com.mongodb.TransactionOptions;
import dev.morphia.experimental.MorphiaSession;
import dev.morphia.test.models.Rectangle;
import dev.morphia.test.models.User;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;

import static com.mongodb.ClientSessionOptions.builder;
import static com.mongodb.WriteConcern.MAJORITY;
import static dev.morphia.query.experimental.updates.UpdateOperators.inc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

//@Tags(@Tag("transactions"))
public class TestTransactions extends TestBase {
    @BeforeMethod
    public void before() {
        checkMinServerVersion(4.0);
        assumeTrue(isReplicaSet(), "These tests require a replica set");
        getDatastore().save(new Rectangle(1, 1));
        getDatastore().find(Rectangle.class).findAndDelete();
        getDatastore().save(new User("", LocalDate.now()));
        getDatastore().find(User.class).findAndDelete();
    }

    @Test
    public void delete() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDatastore().save(rectangle);

        getDatastore().withTransaction(builder()
                                           .defaultTransactionOptions(TransactionOptions.builder()
                                                                                        .writeConcern(MAJORITY)
                                                                                        .build())
                                           .build(), (session) -> {

            assertNotNull(getDatastore().find(Rectangle.class).first());
            assertNotNull(session.find(Rectangle.class).first());

            session.delete(rectangle);

            assertNotNull(getDatastore().find(Rectangle.class).first());
            assertNull(session.find(Rectangle.class).first());
            return null;
        });

        assertNull(getDatastore().find(Rectangle.class).first());
    }

    @Test
    public void insert() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDatastore().withTransaction((session) -> {
            session.insert(rectangle);

            assertNull(getDatastore().find(Rectangle.class).first());
            assertEquals(session.find(Rectangle.class).count(), 1);

            return null;
        });

        assertNotNull(getDatastore().find(Rectangle.class).first());
    }

    @Test
    public void insertList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
            new Rectangle(1, 1));

        getDatastore().withTransaction((session) -> {
            session.insert(rectangles);

            assertNull(getDatastore().find(Rectangle.class).first());
            assertEquals(session.find(Rectangle.class).iterator().toList(), rectangles);

            return null;
        });

        assertEquals(getDatastore().find(Rectangle.class).count(), 2);
    }

    @Test
    public void manual() {
        try (MorphiaSession session = getDatastore().startSession()) {
            session.startTransaction();

            Rectangle rectangle = new Rectangle(1, 1);
            session.save(rectangle);

            session.save(new User("transactions", LocalDate.now()));

            assertNull(getDatastore().find(Rectangle.class).first());
            assertNull(getDatastore().find(User.class).first());
            assertNotNull(session.find(Rectangle.class).first());
            assertNotNull(session.find(User.class).first());

            session.commitTransaction();
        }

        assertNotNull(getDatastore().find(Rectangle.class).first());
        assertNotNull(getDatastore().find(User.class).first());
    }

    @Test
    public void merge() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDatastore().save(rectangle);
        assertEquals(getDatastore().find(Rectangle.class).count(), 1);

        getDatastore().withTransaction((session) -> {

            assertEquals(getDatastore().find(Rectangle.class).first(), new Rectangle(1, 1));
            assertEquals(session.find(Rectangle.class).first(), new Rectangle(1, 1));

            rectangle.setWidth(20);
            session.merge(rectangle);

            assertEquals(getDatastore().find(Rectangle.class).first().getWidth(), 1, 0.5);
            assertEquals(session.find(Rectangle.class).first().getWidth(), 20, 0.5);

            return null;
        });

        assertEquals(getDatastore().find(Rectangle.class).first().getWidth(), 20, 0.5);
    }

    @Test
    public void modify() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDatastore().withTransaction((session) -> {
            session.save(rectangle);

            assertNull(getDatastore().find(Rectangle.class).first());

            Rectangle modified = session.find(Rectangle.class)
                                        .modify(inc("width", 13))
                                        .execute();

            assertNull(getDatastore().find(Rectangle.class).first());
            assertEquals(rectangle.getWidth(), modified.getWidth(), 0.5);
            assertEquals(rectangle.getWidth() + 13, session.find(Rectangle.class)
                                                           .first().getWidth(), 0.5);

            return null;
        });

        assertEquals(getDatastore().find(Rectangle.class).first().getWidth(), rectangle.getWidth() + 13, 0.5);
    }

    @Test
    public void remove() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDatastore().save(rectangle);

        getDatastore().withTransaction((session) -> {

            assertNotNull(getDatastore().find(Rectangle.class).first());
            assertNotNull(session.find(Rectangle.class).first());

            session.find(Rectangle.class)
                   .delete();

            assertNotNull(getDatastore().find(Rectangle.class).first());
            assertNull(session.find(Rectangle.class).first());
            return null;
        });

        assertNull(getDatastore().find(Rectangle.class).first());
    }

    @Test
    public void save() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDatastore().withTransaction((session) -> {
            session.save(rectangle);

            assertNull(getDatastore().find(Rectangle.class).first());
            assertNotNull(session.find(Rectangle.class).first());

            rectangle.setWidth(42);
            session.save(rectangle);

            assertNull(getDatastore().find(Rectangle.class).first());
            assertEquals(session.find(Rectangle.class).first().getWidth(), 42, 0.5);

            return null;
        });

        assertNotNull(getDatastore().find(Rectangle.class).first());
    }

    @Test
    public void saveList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
            new Rectangle(1, 1));

        getDatastore().withTransaction((session) -> {
            session.save(rectangles);

            assertNull(getDatastore().find(Rectangle.class).first());
            assertEquals(session.find(Rectangle.class).count(), 2);

            return null;
        });

        assertEquals(getDatastore().find(Rectangle.class).count(), 2);
    }

    @Test
    public void update() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDatastore().withTransaction((session) -> {
            session.save(rectangle);

            assertNull(getDatastore().find(Rectangle.class).first());

            session.find(Rectangle.class)
                   .update(inc("width", 13))
                   .execute();

            assertEquals(session.find(Rectangle.class).first().getWidth(), rectangle.getWidth() + 13, 0.5);

            assertNull(getDatastore().find(Rectangle.class).first());
            return null;
        });

        assertEquals(getDatastore().find(Rectangle.class).first().getWidth(), rectangle.getWidth() + 13, 0.5);
    }

}
