package dev.morphia;

import dev.morphia.testmodel.Rectangle;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestTransactions extends TestBase {
    @Before
    public void before() {
        Assume.assumeTrue(isReplicaSet());
        getMapper().map(Rectangle.class);
        getDs().ensureIndexes();
        getDs().save(new Rectangle(1, 1));
        getDs().find(Rectangle.class).delete();
    }

    @Test
    public void delete() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDs().save(rectangle);

        getDs().withTransaction((session) -> {

            Assert.assertNotNull(getDs().find(Rectangle.class).first());
            Assert.assertNotNull(session.find(Rectangle.class).first());

            session.delete(rectangle);

            Assert.assertNotNull(getDs().find(Rectangle.class).first());
            Assert.assertNull(session.find(Rectangle.class).first());
            return null;
        });

        Assert.assertNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void insert() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction((session) -> {
            session.insert(rectangle);

            Assert.assertNull(getDs().find(Rectangle.class).first());
            Assert.assertEquals(1, session.find(Rectangle.class).count());

            return null;
        });

        Assert.assertNotNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void insertList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
            new Rectangle(1, 1));

        getDs().withTransaction((session) -> {
            session.insert(rectangles);

            Assert.assertNull(getDs().find(Rectangle.class).first());
            Assert.assertEquals(rectangles, session.find(Rectangle.class).execute().toList());

            return null;
        });

        Assert.assertEquals(2, getDs().find(Rectangle.class).count());
    }

    @Test
    public void merge() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDs().save(rectangle);

        getDs().withTransaction((session) -> {

            Assert.assertEquals(rectangle, getDs().find(Rectangle.class).first());
            Assert.assertEquals(rectangle, session.find(Rectangle.class).first());

            rectangle.setWidth(20);
            session.merge(rectangle);

            Assert.assertEquals(1, getDs().find(Rectangle.class).first().getWidth(), 0.5);
            Assert.assertEquals(20, session.find(Rectangle.class).first().getWidth(), 0.5);

            return null;
        });

        Assert.assertEquals(20, getDs().find(Rectangle.class).first().getWidth(), 0.5);
    }

    @Test
    public void modify() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction((session) -> {
            session.save(rectangle);

            Assert.assertNull(getDs().find(Rectangle.class).first());

            Rectangle modified = session.find(Rectangle.class)
                                        .modify()
                                        .inc("width", 13)
                                        .execute();

            Assert.assertNull(getDs().find(Rectangle.class).first());
            Assert.assertEquals(rectangle.getWidth() + 13, modified.getWidth(), 0.5);
            Assert.assertEquals(rectangle.getWidth() + 13, session.find(Rectangle.class)
                                                                  .first().getWidth(), 0.5);

            return null;
        });

        Assert.assertEquals(rectangle.getWidth() + 13, getDs().find(Rectangle.class)
                                                              .first().getWidth(), 0.5);
    }

    @Test
    public void remove() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDs().save(rectangle);

        getDs().withTransaction((session) -> {

            Assert.assertNotNull(getDs().find(Rectangle.class).first());
            Assert.assertNotNull(session.find(Rectangle.class).first());

            session.find(Rectangle.class)
                   .remove();

            Assert.assertNotNull(getDs().find(Rectangle.class).first());
            Assert.assertNull(session.find(Rectangle.class).first());
            return null;
        });

        Assert.assertNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void save() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction((session) -> {
            session.save(rectangle);

            Assert.assertNull(getDs().find(Rectangle.class).first());
            Assert.assertNotNull(session.find(Rectangle.class).first());

            return null;
        });

        Assert.assertNotNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void saveList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
            new Rectangle(1, 1));

        getDs().withTransaction((session) -> {
            session.save(rectangles);

            Assert.assertNull(getDs().find(Rectangle.class).first());
            Assert.assertEquals(2, session.find(Rectangle.class).count());

            return null;
        });

        Assert.assertEquals(2, getDs().find(Rectangle.class).count());
    }

    @Test
    public void update() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction((session) -> {
            session.save(rectangle);

            Assert.assertNull(getDs().find(Rectangle.class).first());

            session.find(Rectangle.class)
                   .update()
                   .inc("width", 13)
                   .execute();

            Assert.assertEquals(rectangle.getWidth() + 13, session.find(Rectangle.class)
                                                                  .first().getWidth(), 0.5);

            Assert.assertNull(getDs().find(Rectangle.class).first());
            return null;
        });

        Assert.assertEquals(rectangle.getWidth() + 13, getDs().find(Rectangle.class)
                                                              .first().getWidth(), 0.5);
    }
}
