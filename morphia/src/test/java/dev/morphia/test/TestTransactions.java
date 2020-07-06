package dev.morphia.test;

import dev.morphia.experimental.MorphiaSession;
import dev.morphia.test.models.Rectangle;
import dev.morphia.test.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static dev.morphia.query.experimental.updates.UpdateOperators.inc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tags(@Tag("transactions"))
public class TestTransactions extends TestBase {
    @BeforeEach
    public void before() {
        checkMinServerVersion(4.0);
        assumeTrue(isReplicaSet());
        getDs().save(new Rectangle(1, 1));
        getDs().find(Rectangle.class).findAndDelete();
        getDs().save(new User("", new Date()));
        getDs().find(User.class).findAndDelete();
    }

    @Test
    public void delete() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDs().save(rectangle);

        getDs().withTransaction((session) -> {

            assertNotNull(getDs().find(Rectangle.class).first());
            assertNotNull(session.find(Rectangle.class).first());

            session.delete(rectangle);

            assertNotNull(getDs().find(Rectangle.class).first());
            assertNull(session.find(Rectangle.class).first());
            return null;
        });

        assertNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void insert() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction((session) -> {
            session.insert(rectangle);

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(1, session.find(Rectangle.class).count());

            return null;
        });

        assertNotNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void insertList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
            new Rectangle(1, 1));

        getDs().withTransaction((session) -> {
            session.insert(rectangles);

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(rectangles, session.find(Rectangle.class).iterator().toList());

            return null;
        });

        assertEquals(2, getDs().find(Rectangle.class).count());
    }

    @Test
    public void manual() {
        try (MorphiaSession session = getDs().startSession()) {
            session.startTransaction();

            Rectangle rectangle = new Rectangle(1, 1);
            session.save(rectangle);

            session.save(new User("transactions", new Date()));

            assertNull(getDs().find(Rectangle.class).first());
            assertNull(getDs().find(User.class).first());
            assertNotNull(session.find(Rectangle.class).first());
            assertNotNull(session.find(User.class).first());

            session.commitTransaction();
        }

        assertNotNull(getDs().find(Rectangle.class).first());
        assertNotNull(getDs().find(User.class).first());
    }

    @Test
    public void merge() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDs().save(rectangle);

        getDs().withTransaction((session) -> {

            assertEquals(rectangle, getDs().find(Rectangle.class).first());
            assertEquals(rectangle, session.find(Rectangle.class).first());

            rectangle.setWidth(20);
            session.merge(rectangle);

            assertEquals(1, getDs().find(Rectangle.class).first().getWidth(), 0.5);
            assertEquals(20, session.find(Rectangle.class).first().getWidth(), 0.5);

            return null;
        });

        assertEquals(20, getDs().find(Rectangle.class).first().getWidth(), 0.5);
    }

    @Test
    public void modify() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction((session) -> {
            session.save(rectangle);

            assertNull(getDs().find(Rectangle.class).first());

            Rectangle modified = session.find(Rectangle.class)
                                        .modify(inc("width", 13))
                                        .execute();

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(rectangle.getWidth() + 13, modified.getWidth(), 0.5);
            assertEquals(rectangle.getWidth() + 13, session.find(Rectangle.class)
                                                           .first().getWidth(), 0.5);

            return null;
        });

        assertEquals(rectangle.getWidth() + 13, getDs().find(Rectangle.class)
                                                       .first().getWidth(), 0.5);
    }

    @Test
    public void remove() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDs().save(rectangle);

        getDs().withTransaction((session) -> {

            assertNotNull(getDs().find(Rectangle.class).first());
            assertNotNull(session.find(Rectangle.class).first());

            session.find(Rectangle.class)
                   .delete();

            assertNotNull(getDs().find(Rectangle.class).first());
            assertNull(session.find(Rectangle.class).first());
            return null;
        });

        assertNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void save() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction((session) -> {
            session.save(rectangle);

            assertNull(getDs().find(Rectangle.class).first());
            assertNotNull(session.find(Rectangle.class).first());

            rectangle.setWidth(42);
            session.save(rectangle);

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(42, session.find(Rectangle.class).first().getWidth(), 0.5);

            return null;
        });

        assertNotNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void saveList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
            new Rectangle(1, 1));

        getDs().withTransaction((session) -> {
            session.save(rectangles);

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(2, session.find(Rectangle.class).count());

            return null;
        });

        assertEquals(2, getDs().find(Rectangle.class).count());
    }

    @Test
    public void update() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction((session) -> {
            session.save(rectangle);

            assertNull(getDs().find(Rectangle.class).first());

            session.find(Rectangle.class)
                   .update(inc("width", 13))
                   .execute();

            assertEquals(rectangle.getWidth() + 13, session.find(Rectangle.class)
                                                           .first().getWidth(), 0.5);

            assertNull(getDs().find(Rectangle.class).first());
            return null;
        });

        assertEquals(rectangle.getWidth() + 13, getDs().find(Rectangle.class)
                                                       .first().getWidth(), 0.5);
    }

}
