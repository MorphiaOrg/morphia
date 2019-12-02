package dev.morphia;

import dev.morphia.testmodel.Rectangle;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestTransactions extends TestBase {
    @Before
    public void before() {
        getMapper().map(Rectangle.class);
        getDs().ensureIndexes();
        getDs().save(new Rectangle(1, 1));
        getDs().find(Rectangle.class).delete();
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
