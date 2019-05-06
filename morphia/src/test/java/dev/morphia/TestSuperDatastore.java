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

import dev.morphia.testmodel.Circle;
import dev.morphia.testmodel.Rectangle;
import org.bson.types.ObjectId;
import org.junit.Test;

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
        getDs().delete(getDs().find(Rectangle.class).filter("_id", 1));

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
        getDs().delete(getDs().find(Circle.class).filter("_id", rectangleId));

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
        getDs().delete(getDs().find(Rectangle.class).filter("_id", id));

        // then
        assertEquals(0, getDs().find(Rectangle.class).count());
    }

}
