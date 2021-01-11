package dev.morphia.test.mapping;

import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import dev.morphia.test.TestBase;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class TestLifecycles extends TestBase {
    @Test
    public void testWithGeoJson() {
        final Polygon polygon = new Polygon(
            List.of(new Position(0d, 0d), new Position(1d, 1d), new Position(2d, 2d), new Position(3d, 3d), new Position(0d, 0d)));
        getDs().save(new HoldsPolygon(ObjectId.get(), polygon));

        Assert.assertFalse(HoldsPolygon.lifecycle);
        Assert.assertNotNull(getDs().find(HoldsPolygon.class).first());
        Assert.assertTrue(HoldsPolygon.lifecycle);
    }

    @Entity(value = "polygon", useDiscriminator = false)
    private static class HoldsPolygon {
        @Id
        private final ObjectId id;
        private final Polygon polygon;
        private static boolean lifecycle = false;

        protected HoldsPolygon() {
            id = null;
            polygon = null;
        }

        public HoldsPolygon(ObjectId id, Polygon polygon) {
            this.id = id;
            this.polygon = polygon;
        }

        public ObjectId getId() {
            return id;
        }

        public Polygon getPolygon() {
            return polygon;
        }

        @PostLoad
        void somePostLoadMethod() {
            lifecycle = true;
        }
    }
}
