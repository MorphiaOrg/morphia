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
        getDs().find(Rectangle.class).filter(eq("_id", 1)).findAndDelete();

        // then
        assertEquals(1, getDs().find(Rectangle.class).count());
    }

    @Test
    public void testDeleteWillRemoveAnyDocumentWithAMatchingId() {
        final Rectangle rect = new Rectangle(10, 10);
        getDs().save(rect);

        getDs().save(new Circle());

        assertEquals(1, getDs().find(Rectangle.class).count());

        // when
        getDs().find(Circle.class).filter(eq("_id", rect.getId())).findAndDelete();

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
        getDs().find(Rectangle.class).filter(eq("_id", id)).findAndDelete();

        // then
        assertEquals(0, getDs().find(Rectangle.class).count());
    }

}
