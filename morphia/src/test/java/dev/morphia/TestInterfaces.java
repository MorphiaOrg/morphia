/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */


package dev.morphia;


import com.mongodb.client.MongoCollection;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.cache.DefaultEntityCache;
import dev.morphia.testmodel.Circle;
import dev.morphia.testmodel.Rectangle;
import dev.morphia.testmodel.Shape;
import dev.morphia.testmodel.ShapeShifter;
import org.bson.Document;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Olafur Gauti Gudmundsson
 */
public class TestInterfaces extends TestBase {

    @Test
    public void testDynamicInstantiation() {
        final MongoCollection<Document> shapes = getDatabase().getCollection("shapes");
        final MongoCollection<Document> shapeshifters = getDatabase().getCollection("shapeshifters");

        Mapper.map(ShapeShifter.class);

        final Shape rectangle = new Rectangle(2, 5);

        final Document rectangleDbObj = TestBase.toDocument(rectangle);
        shapes.insertOne(rectangleDbObj);

        final Document rectangleDbObjLoaded = shapes.find(new Document("_id", rectangleDbObj.get("_id"))).first();
        final Shape rectangleLoaded = TestBase.fromDocument(getDs(), Shape.class, rectangleDbObjLoaded, new DefaultEntityCache());

        assertEquals(rectangle.getArea(), rectangleLoaded.getArea(), 0.0);
        assertTrue(rectangleLoaded instanceof Rectangle);

        final ShapeShifter shifter = new ShapeShifter();
        shifter.setReferencedShape(rectangleLoaded);
        shifter.setMainShape(new Circle(2.2));
        shifter.getAvailableShapes().add(new Rectangle(3, 3));
        shifter.getAvailableShapes().add(new Circle(4.4));

        final Document shifterDbObj = TestBase.toDocument(shifter);
        shapeshifters.insertOne(shifterDbObj);

        final Document shifterDbObjLoaded = shapeshifters.find(new Document("_id", shifterDbObj.get("_id"))).first();
        final ShapeShifter shifterLoaded = TestBase.fromDocument(getDs(), ShapeShifter.class, shifterDbObjLoaded,
                                                                     new DefaultEntityCache());
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
