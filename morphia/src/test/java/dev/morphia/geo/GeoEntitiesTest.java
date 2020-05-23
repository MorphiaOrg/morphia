package dev.morphia.geo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import dev.morphia.TestBase;
import dev.morphia.query.FindOptions;
import dev.morphia.testutil.JSONMatcher;
import org.junit.Test;

import static dev.morphia.geo.GeoJson.lineString;
import static dev.morphia.geo.GeoJson.multiPolygon;
import static dev.morphia.geo.GeoJson.point;
import static dev.morphia.geo.GeoJson.polygon;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test driving features for Issue 643 - add support for saving entities with GeoJSON.
 */
public class GeoEntitiesTest extends TestBase {
    @Test
    public void shouldConvertPointCorrectlyToDBObject() {
        // given
        City city = new City("New City", point(3.0, 7.0));

        // when
        DBObject dbObject = getMorphia().toDBObject(city);

        assertThat(dbObject, is(notNullValue()));
        assertThat(dbObject.toString(), JSONMatcher.jsonEqual("  {"
                                                              + " name: 'New City',"
                                                              + " className: 'dev.morphia.geo.City',"
                                                              + " location:  "
                                                              + " {"
                                                              + "  type: 'Point', "
                                                              + "  coordinates: [7.0, 3.0]"
                                                              + " }"
                                                              + "}"));
    }

    @Test
    public void shouldRetrieveGeoCollectionType() {
        // given
        String name = "What, everything?";
        Point point = point(3.0, 7.0);
        LineString lineString = lineString(point(1, 2), point(3, 5), point(19, 13));
        Polygon polygonWithHoles = polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                           lineString(point(1.5, 2.0), point(1.9, 2.0), point(1.9, 1.8), point(1.5, 2.0)),
                                           lineString(point(2.2, 2.1), point(2.4, 1.9), point(2.4, 1.7), point(2.1, 1.8), point(2.2, 2.1)));
        MultiPoint multiPoint = GeoJson.multiPoint(point(1, 2), point(3, 5), point(19, 13));
        MultiLineString multiLineString = GeoJson.multiLineString(lineString(point(1, 2), point(3, 5), point(19, 13)),
                                                                  lineString(point(1.5, 2.0),
                                                                             point(1.9, 2.0),
                                                                             point(1.9, 1.8),
                                                                             point(1.5, 2.0)));
        MultiPolygon multiPolygon = multiPolygon(polygon(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                                 polygon(lineString(point(1.2, 3.0), point(2.5, 4.5), point(6.7, 1.9), point(1.2, 3.0)),
                                                         lineString(point(3.5, 2.4), point(1.7, 2.8), point(3.5, 2.4))));
        GeometryCollection geometryCollection = GeoJson.geometryCollection(point, lineString, polygonWithHoles, multiPoint,
                                                                           multiLineString, multiPolygon);
        AllTheThings allTheThings = new AllTheThings(name, geometryCollection);
        getDs().save(allTheThings);

        // when
        AllTheThings found = getDs().find(AllTheThings.class).field("name").equal(name).find(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(allTheThings));
    }

    @Test
    public void shouldRetrieveGeoJsonLineString() {
        // given
        Route route = new Route("My Route", lineString(point(1, 2), point(3, 5), point(19, 13)));
        getDs().save(route);

        // when
        Route found = getDs().find(Route.class).field("name").equal("My Route").find(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(route));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiLineString() {
        // given
        String name = "Many Paths";
        Paths paths = new Paths(name, GeoJson.multiLineString(lineString(point(1, 2), point(3, 5), point(19, 13)),
                                                              lineString(point(1.5, 2.0),
                                                                         point(1.9, 2.0),
                                                                         point(1.9, 1.8),
                                                                         point(1.5, 2.0))));
        getDs().save(paths);

        // when
        Paths found = getDs().find(Paths.class).field("name").equal(name).find(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(paths));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiPoint() {
        // given
        String name = "My stores";
        Stores stores = new Stores(name, GeoJson.multiPoint(point(1, 2), point(3, 5), point(19, 13)));
        getDs().save(stores);

        // when
        Stores found = getDs().find(Stores.class).field("name").equal(name).find(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(stores));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiPolygon() {
        // given
        String name = "All these shapes";
        Polygon polygonWithHoles = polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                           lineString(point(1.5, 2.0), point(1.9, 2.0), point(1.9, 1.8), point(1.5, 2.0)),
                                           lineString(point(2.2, 2.1), point(2.4, 1.9), point(2.4, 1.7), point(2.1, 1.8), point(2.2, 2.1)));
        Regions regions = new Regions(name, multiPolygon(polygon(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                                         polygonWithHoles));
        getDs().save(regions);

        // when
        Regions found = getDs().find(Regions.class).field("name").equal(name).find(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(regions));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiRingPolygon() {
        // given
        String polygonName = "A polygon with holes";
        Polygon polygonWithHoles = polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                           lineString(point(1.5, 2.0), point(1.9, 2.0), point(1.9, 1.8), point(1.5, 2.0)),
                                           lineString(point(2.2, 2.1), point(2.4, 1.9), point(2.4, 1.7), point(2.1, 1.8), point(2.2, 2.1)));
        Area area = new Area(polygonName, polygonWithHoles);
        getDs().save(area);

        // when
        Area found = getDs().find(Area.class).field("name").equal(polygonName).find(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(area));
    }

    @Test
    public void shouldRetrieveGeoJsonPoint() {
        // given
        City city = new City("New City", point(3.0, 7.0));
        getDs().save(city);

        // when
        City found = getDs().find(City.class).field("name").equal("New City").find(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(city));
    }

    @Test
    public void shouldRetrieveGeoJsonPolygon() {
        // given
        Area area = new Area("The Area", polygon(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)));
        getDs().save(area);

        // when
        Area found = getDs().find(Area.class).field("name").equal("The Area").find(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(area));
    }

    @Test
    public void shouldSaveAnEntityWithAGeoCollectionType() {
        // given
        String name = "What, everything?";
        Point point = point(3.0, 7.0);
        LineString lineString = lineString(point(1, 2), point(3, 5), point(19, 13));
        Polygon polygonWithHoles = polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                           lineString(point(1.5, 2.0), point(1.9, 2.0), point(1.9, 1.8), point(1.5, 2.0)),
                                           lineString(point(2.2, 2.1), point(2.4, 1.9), point(2.4, 1.7), point(2.1, 1.8), point(2.2, 2.1)));
        MultiPoint multiPoint = GeoJson.multiPoint(point(1, 2), point(3, 5), point(19, 13));
        MultiLineString multiLineString = GeoJson.multiLineString(lineString(point(1, 2), point(3, 5), point(19, 13)),
                                                                  lineString(point(1.5, 2.0),
                                                                             point(1.9, 2.0),
                                                                             point(1.9, 1.8),
                                                                             point(1.5, 2.0)));
        MultiPolygon multiPolygon = multiPolygon(polygon(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                                 polygon(lineString(point(1.2, 3.0), point(2.5, 4.5), point(6.7, 1.9), point(1.2, 3.0)),
                                                         lineString(point(3.5, 2.4), point(1.7, 2.8), point(3.5, 2.4))));

        GeometryCollection geometryCollection = GeoJson.geometryCollection(point, lineString, polygonWithHoles, multiPoint, multiLineString,
                                                                           multiPolygon);
        AllTheThings allTheThings = new AllTheThings(name, geometryCollection);

        // when
        getDs().save(allTheThings);
    }

    @Test
    public void shouldSaveAnEntityWithALineStringGeoJsonType() {
        // given
        Route route = new Route("My Route", lineString(point(1, 2), point(3, 5), point(19, 13)));

        // when
        getDs().save(route);

        // then use the underlying driver to ensure it was persisted correctly to the database
        DBObject storedRoute = getDs().getCollection(Route.class).findOne(new BasicDBObject("name", "My Route"),
                                                                          new BasicDBObject("_id", 0).append("className", 0));
        assertThat(storedRoute, is(notNullValue()));
        // lat/long is always long/lat on the server
        assertThat(storedRoute.toString(), JSONMatcher.jsonEqual("  {"
                                                                 + " name: 'My Route',"
                                                                 + " route:"
                                                                 + " {"
                                                                 + "  type: 'LineString', "
                                                                 + "  coordinates: [ [ 2.0,  1.0],"
                                                                 + "                 [ 5.0,  3.0],"
                                                                 + "                 [13.0, 19.0] ]"
                                                                 + " }"
                                                                 + "}"));
    }

    @Test
    public void shouldSaveAnEntityWithALocationStoredAsAMultiPoint() {
        // given
        String name = "My stores";
        Stores stores = new Stores(name, GeoJson.multiPoint(point(1, 2), point(3, 5), point(19, 13)));

        // when
        getDs().save(stores);
    }

    @Test
    public void shouldSaveAnEntityWithALocationStoredAsAPoint() {
        // given
        City city = new City("New City", point(3.0, 7.0));

        // when
        getDs().save(city);

        // then use the underlying driver to ensure it was persisted correctly to the database
        DBObject storedCity = getDs().getCollection(City.class).findOne(new BasicDBObject("name", "New City"),
                                                                        new BasicDBObject("_id", 0).append("className", 0));
        assertThat(storedCity, is(notNullValue()));
        assertThat(storedCity.toString(), JSONMatcher.jsonEqual("  {"
                                                                + " name: 'New City',"
                                                                + " location:  "
                                                                + " {"
                                                                + "  type: 'Point', "
                                                                + "  coordinates: [7.0, 3.0]"
                                                                + " }"
                                                                + "}"));
    }

    @Test
    public void shouldSaveAnEntityWithAMultiLineStringGeoJsonType() {
        // given
        String name = "Many Paths";
        Paths paths = new Paths(name, GeoJson.multiLineString(lineString(point(1, 2), point(3, 5), point(19, 13)),
                                                              lineString(point(1.5, 2.0),
                                                                         point(1.9, 2.0),
                                                                         point(1.9, 1.8),
                                                                         point(1.5, 2.0))));

        // when
        getDs().save(paths);

        // then use the underlying driver to ensure it was persisted correctly to the database
        DBObject storedPaths = getDs().getCollection(Paths.class).findOne(new BasicDBObject("name", name),
                                                                          new BasicDBObject("_id", 0).append("className", 0));
        assertThat(storedPaths, is(notNullValue()));
        // lat/long is always long/lat on the server
        assertThat(storedPaths.toString(), JSONMatcher.jsonEqual("  {"
                                                                 + " name: '" + name + "',"
                                                                 + " paths:"
                                                                 + " {"
                                                                 + "  type: 'MultiLineString', "
                                                                 + "  coordinates: "
                                                                 + "     [ [ [ 2.0,  1.0],"
                                                                 + "         [ 5.0,  3.0],"
                                                                 + "         [13.0, 19.0] "
                                                                 + "       ], "
                                                                 + "       [ [ 2.0, 1.5],"
                                                                 + "         [ 2.0, 1.9],"
                                                                 + "         [ 1.8, 1.9],"
                                                                 + "         [ 2.0, 1.5] "
                                                                 + "       ]"
                                                                 + "     ]"
                                                                 + " }"
                                                                 + "}"));
    }

    @Test
    public void shouldSaveAnEntityWithAMultiPolygonGeoJsonType() {
        // given
        String name = "All these shapes";
        Polygon polygonWithHoles = polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                           lineString(point(1.5, 2.0), point(1.9, 2.0), point(1.9, 1.8), point(1.5, 2.0)),
                                           lineString(point(2.2, 2.1), point(2.4, 1.9), point(2.4, 1.7), point(2.1, 1.8),
                                                      point(2.2, 2.1)));
        Regions regions = new Regions(name, multiPolygon(polygon(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                                         polygonWithHoles));

        // when
        getDs().save(regions);
    }

    @Test
    public void shouldSaveAnEntityWithAPolygonContainingInteriorRings() {
        // given
        String polygonName = "A polygon with holes";
        Polygon polygonWithHoles = polygon(lineString(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)),
                                           lineString(point(1.5, 2.0), point(1.9, 2.0), point(1.9, 1.8), point(1.5, 2.0)),
                                           lineString(point(2.2, 2.1), point(2.4, 1.9), point(2.4, 1.7), point(2.1, 1.8), point(2.2, 2.1)));
        Area area = new Area(polygonName, polygonWithHoles);

        // when
        getDs().save(area);
    }

    @Test
    public void shouldSaveAnEntityWithAPolygonGeoJsonType() {
        // given
        Area area = new Area("The Area", polygon(point(1.1, 2.0), point(2.3, 3.5), point(3.7, 1.0), point(1.1, 2.0)));

        // when
        getDs().save(area);

        // then use the underlying driver to ensure it was persisted correctly to the database
        DBObject storedArea = getDs().getCollection(Area.class).findOne(new BasicDBObject("name", "The Area"),
                                                                        new BasicDBObject("_id", 0)
                                                                            .append("className", 0)
                                                                            .append("area.className", 0));
        assertThat(storedArea, is(notNullValue()));
        assertThat(storedArea.toString(), JSONMatcher.jsonEqual("  {"
                                                                + " name: 'The Area',"
                                                                + " area:  "
                                                                + " {"
                                                                + "  type: 'Polygon', "
                                                                + "  coordinates: [ [ [ 2.0, 1.1],"
                                                                + "                   [ 3.5, 2.3],"
                                                                + "                   [ 1.0, 3.7],"
                                                                + "                   [ 2.0, 1.1] ] ]"
                                                                + " }"
                                                                + "}"));
    }

    @Test
    public void shouldSaveAnEntityWithNullPoints() {
        getDs().save(new City("New City", null));

        DBObject storedCity = getDs().getCollection(City.class)
                                     .findOne(new BasicDBObject("name", "New City"),
                                              new BasicDBObject("_id", 0)
                                                  .append("className", 0));
        assertThat(storedCity, is(notNullValue()));
        assertThat(storedCity.toString(), JSONMatcher.jsonEqual("{ name: 'New City'}"));
    }


    @SuppressWarnings("UnusedDeclaration")
    private static final class AllTheThings {
        private GeometryCollection everything;
        private String name;

        private AllTheThings() {
        }

        private AllTheThings(final String name, final GeometryCollection everything) {
            this.name = name;
            this.everything = everything;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AllTheThings that = (AllTheThings) o;

            if (!everything.equals(that.everything)) {
                return false;
            }
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            int result = everything.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "AllTheThings{"
                   + "everything=" + everything
                   + ", name='" + name + '\''
                   + '}';
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static final class Paths {
        private String name;
        private MultiLineString paths;

        private Paths() {
        }

        private Paths(final String name, final MultiLineString paths) {
            this.name = name;
            this.paths = paths;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + paths.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Paths paths1 = (Paths) o;

            if (!name.equals(paths1.name)) {
                return false;
            }
            return paths.equals(paths1.paths);
        }


        @Override
        public String toString() {
            return "Paths{"
                   + "name='" + name + '\''
                   + ", paths=" + paths
                   + '}';
        }
    }
}
