package dev.morphia;

import com.mongodb.client.MongoCollection;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MappedClass;
import dev.morphia.testmodel.Circle;
import dev.morphia.testmodel.Rectangle;
import dev.morphia.testmodel.Shape;
import dev.morphia.testmodel.ShapeShifter;
import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestInterfaces extends TestBase {

    @Test
    @Category(Reference.class)
    public void testDynamicInstantiation() {
        final MongoCollection<Document> shapes = getDatabase().getCollection("shapes");
        final MongoCollection<Document> shapeshifters = getDatabase().getCollection("shapeshifters");

        List<MappedClass> map = getMapper().map(ShapeShifter.class);

        final Shape rectangle = new Rectangle(2, 5);

        final Document rectDocument = getMapper().toDocument(rectangle);
        shapes.insertOne(rectDocument);

        Document loaded = shapes.find(new Document("_id", rectDocument.get("_id"))).first();
        final Shape rectangleLoaded = getMapper().fromDocument(Shape.class, loaded);

        assertEquals(rectangle.getArea(), rectangleLoaded.getArea(), 0.0);
        assertTrue(rectangleLoaded instanceof Rectangle);

        final ShapeShifter shifter = new ShapeShifter();
        shifter.setReferencedShape(rectangleLoaded);
        shifter.setMainShape(new Circle(2.2));
        shifter.getAvailableShapes().add(new Rectangle(3, 3));
        shifter.getAvailableShapes().add(new Circle(4.4));

        final Document document = getMapper().toDocument(shifter);
        shapeshifters.insertOne(document);

        loaded = shapeshifters.find(new Document("_id", document.get("_id"))).first();
        final ShapeShifter shifterLoaded = getMapper().fromDocument(ShapeShifter.class, loaded);
        assertNotNull(shifterLoaded);
        assertNotNull(shifterLoaded.getReferencedShape());
        assertNotNull(shifterLoaded.getReferencedShape().getArea());
        assertNotNull(rectangle);
        assertNotNull(rectangle.getArea());

        assertEquals(rectangle.getArea(), shifterLoaded.getReferencedShape().getArea(), 0.0);
        assertTrue(shifterLoaded.getReferencedShape() instanceof Rectangle);
        assertEquals(shifter.getMainShape().getArea(), shifterLoaded.getMainShape().getArea(), 0.0);
        assertEquals(shifter.getAvailableShapes().size(), shifterLoaded.getAvailableShapes().size());
    }
}
