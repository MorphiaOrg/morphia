package dev.morphia;

import dev.morphia.testmodel.Rectangle;
import org.junit.Assert;
import org.junit.Test;

public class TestTransactions extends TestBase {
    @Test
    public void save() {
        getMapper().map(Rectangle.class);
        getDs().ensureIndexes();
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
    public void transactions() {
        getMapper().map(Rectangle.class);
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

            Assert.assertEquals(rectangle, getDs().find(Rectangle.class)
                                                  .first());


            return null;
        });

        Assert.assertEquals(rectangle.getWidth() + 13, getDs().find(Rectangle.class)
                                                              .first().getWidth(), 0.5);
    }
}
