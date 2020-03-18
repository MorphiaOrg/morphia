package dev.morphia;

import dev.morphia.testmodel.Circle;
import dev.morphia.testmodel.Rectangle;
import org.bson.types.ObjectId;
import org.junit.Test;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.junit.Assert.assertEquals;

public class TestSuperDatastore extends TestBase {
    @Test
    public void testDeleteDoesNotDeleteAnythingWhenGivenAnIncorrectId() {
        final Rectangle rect = new Rectangle(10, 10);
        ObjectId id = new ObjectId();
        rect.setId(id);

        getDs().save(rect);
        assertEquals(1, getDs().find(Rectangle.class).count());

        // when giving an ID that is not the entity ID.  Note that at the time of writing this will also log a validation warning
        getDs().find(Rectangle.class).filter(eq("_id", 1)).delete();

        // then
        assertEquals(1, getDs().find(Rectangle.class).count());
    }

    @Test
    public void testDeleteWillRemoveAnyDocumentWithAMatchingId() {
        final Rectangle rect = new Rectangle(10, 10);
        ObjectId rectangleId = new ObjectId();
        rect.setId(rectangleId);
        getDs().save(rect);

        final Circle circle = new Circle();
        circle.setId(new ObjectId());
        getDs().save(circle);

        assertEquals(2, getDs().find(Rectangle.class).count());

        // when
        getDs().find(Circle.class).filter(eq("_id", rectangleId)).delete();

        // then
        assertEquals(1, getDs().find(Circle.class).count());
    }

    @Test
    public void testDeleteWithAnEntityTypeAndId() {
        final Rectangle rect = new Rectangle(10, 10);
        ObjectId id = new ObjectId();
        rect.setId(id);

        getDs().save(rect);
        assertEquals(1, getDs().find(Rectangle.class).count());

        // when
        getDs().find(Rectangle.class).filter(eq("_id", id)).delete();

        // then
        assertEquals(0, getDs().find(Rectangle.class).count());
    }

}
