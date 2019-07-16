package dev.morphia.geo;

import com.mongodb.client.model.geojson.GeometryCollection;
import com.mongodb.client.model.geojson.LineString;
import com.mongodb.client.model.geojson.MultiLineString;
import com.mongodb.client.model.geojson.MultiPoint;
import com.mongodb.client.model.geojson.MultiPolygon;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;
import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.query.FindOptions;
import dev.morphia.testutil.JSONMatcher;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Test;

import static java.util.List.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Test driving features for Issue 643 - add support for saving entities with GeoJSON.
 */
@SuppressWarnings("unchecked")
public class GeoEntitiesTest extends TestBase {
    @Test
    public void shouldConvertPointCorrectlyToDBObject() {
        // given
        City city = new City("New City", new Point(new Position(3.0, 7.0)));

        // when
        Document dbObject = getMapper().toDocument(city);

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
        String name = "What, everything?";
        LineString lineString = new LineString(of(
            new Position(1.0, 2.0),
            new Position(3.0, 5.0),
            new Position(19.0, 13.0)));

        Polygon polygonWithHoles = new Polygon(
            new PolygonCoordinates(of(new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0)),

                of(new Position(1.5, 2.0),
                    new Position(1.9, 2.0),
                    new Position(1.9, 1.8),
                    new Position(1.5, 2.0)),

                of(new Position(2.2, 2.1),
                    new Position(2.4, 1.9),
                    new Position(2.4, 1.7),
                    new Position(2.1, 1.8),
                    new Position(2.2, 2.1))));

        MultiPoint multiPoint = new MultiPoint(of(
            new Position(1.0, 2.0),
            new Position(3.0, 5.0),
            new Position(19.0, 13.0)));

        MultiLineString multiLineString = new MultiLineString(of(
            of(new Position(1, 2), new Position(3, 5), new Position(19, 13)),

            of(new Position(1.5, 2.0),
                new Position(1.9, 2.0),
                new Position(1.9, 1.8),
                new Position(1.5, 2.0))));

        MultiPolygon multiPolygon = new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(1.1, 2.0), 
                new Position(2.3, 3.5), 
                new Position(3.7, 1.0),
                new Position(1.1, 2.0)))));
        
        GeometryCollection geometryCollection = new GeometryCollection(of(lineString, polygonWithHoles, multiPoint,
            multiLineString, multiPolygon));
        
        AllTheThings allTheThings = new AllTheThings(name, geometryCollection);
        getDs().save(allTheThings);

        // when
        AllTheThings found = getDs()
                                 .find(AllTheThings.class)
                                 .field("name").equal(name)
                                 .execute(new FindOptions().limit(1))
                                 .tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(allTheThings));
    }

    @Test
    public void shouldRetrieveGeoJsonLineString() {
        // given
        Route route = new Route("My Route", new LineString(of(
            new Position(1, 2),
            new Position(3, 5),
            new Position(19, 13))));
        getDs().save(route);

        // when
        Route found = getDs().find(Route.class).field("name").equal("My Route").execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(route));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiLineString() {
        // given
        String name = "Many Paths";
        Paths paths = new Paths(name, new MultiLineString(
            of(
                of(
                    new Position(1, 2),
                    new Position(3, 5),
                    new Position(19, 13)),

                of(
                    new Position(1.5, 2.0),
                    new Position(1.9, 2.0),
                    new Position(1.9, 1.8),
                    new Position(1.5, 2.0)))));
        
        getDs().save(paths);

        // when
        Paths found = getDs().find(Paths.class).field("name").equal(name).execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(paths));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiPoint() {
        // given
        String name = "My stores";
        Stores stores = new Stores(name, new MultiPoint(of(
            new Position(1, 2), 
            new Position(3, 5), 
            new Position(19, 13))));
        getDs().save(stores);

        // when
        Stores found = getDs().find(Stores.class).field("name").equal(name).execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(stores));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiPolygon() {
        // given
        String name = "All these shapes";
        PolygonCoordinates polygonWithHoles = new PolygonCoordinates(
            of(
                new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0)),

            of(
                new Position(1.5, 2.0),
                new Position(1.9, 2.0),
                new Position(1.9, 1.8),
                new Position(1.5, 2.0)),
            of(
                new Position(2.2, 2.1),
                new Position(2.4, 1.9),
                new Position(2.4, 1.7),
                new Position(2.1, 1.8),
                new Position(2.2, 2.1)));

        Regions regions = new Regions(name, new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0))),
            polygonWithHoles)));
        getDs().save(regions);

        // when
        Regions found = getDs().find(Regions.class).field("name").equal(name).execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(regions));
    }

    @Test
    public void shouldRetrieveGeoJsonMultiRingPolygon() {
        // given
        String polygonName = "A polygon with holes";
        Polygon polygonWithHoles = new Polygon(
            of(
                new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0)),
            of(
                new Position(1.5, 2.0),
                new Position(1.9, 2.0),
                new Position(1.9, 1.8),
                new Position(1.5, 2.0)),
            of(
                new Position(2.2, 2.1),
                new Position(2.4, 1.9),
                new Position(2.4, 1.7),
                new Position(2.1, 1.8),
                new Position(2.2, 2.1)));

        Area area = new Area(polygonName, polygonWithHoles);
        getDs().save(area);

        // when
        Area found = getDs().find(Area.class).field("name").equal(polygonName).execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(area));
    }

    @Test
    public void shouldRetrieveGeoJsonPoint() {
        // given
        City city = new City("New City", new Point(new Position(3.0, 7.0)));
        getDs().save(city);

        // when
        City found = getDs().find(City.class).field("name").equal("New City").execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(city));
    }

    @Test
    public void shouldRetrieveGeoJsonPolygon() {
        // given
        Area area = new Area("The Area", new Polygon(of(
            new Position(2.0, 1.1),
            new Position(3.5, 2.3),
            new Position(1.0, 3.7),
            new Position(2.0, 1.1))));
        getDs().save(area);

        // when
        Area found = getDs().find(Area.class).field("name").equal("The Area").execute(new FindOptions().limit(1)).tryNext();

        // then
        assertThat(found, is(notNullValue()));
        assertThat(found, is(area));
    }

    @Test
    public void shouldSaveAnEntityWithAGeoCollectionType() {
        // given
        String name = "What, everything?";
        Point point = new Point(new Position(3.0, 7.0));

        LineString lineString = new LineString(
            of(
                new Position(2, 1),
                new Position(5, 3),
                new Position(13, 19)));

        Polygon polygonWithHoles = new Polygon(
            of(
                new Position(2.0, 1.1),
                new Position(3.5, 2.3),
                new Position(1.0, 3.7),
                new Position(2.0, 1.1)),

            of(
                new Position(2.0, 1.5),
                new Position(2.0, 1.9),
                new Position(1.8, 1.9),
                new Position(2.0, 1.5)),

            of(
                new Position(2.1, 2.2),
                new Position(1.9, 2.4),
                new Position(1.7, 2.4),
                new Position(1.8, 2.1),
                new Position(2.1, 2.2)));
        
        MultiPoint multiPoint = new MultiPoint(
            of(
                new Position(2, 1),
                new Position(5, 3),
                new Position(13, 19)));
        
        MultiLineString multiLineString = new MultiLineString(of(
            of(new Position(2, 1),
                new Position(5, 3),
                new Position(13, 19)),

            of(new Position(2.0, 1.5),
                new Position(2.0, 1.9),
                new Position(1.8, 1.9),
                new Position(2.0, 1.5))));
        
        MultiPolygon multiPolygon = new MultiPolygon(of(
            new PolygonCoordinates(of(
                new Position(2.0, 1.1),
                new Position(3.5, 2.3),
                new Position(1.0, 3.7),
                new Position(2.0, 1.1))),

            new PolygonCoordinates(
                of(
                    new Position(3.0, 1.2),
                    new Position(4.5, 2.5),
                    new Position(1.9, 6.7),
                    new Position(3.0, 1.2)),
                of(
                    new Position(2.4, 3.5),
                    new Position(2.8, 1.7),
                    new Position(3.0, 2.5),
                    new Position(2.4, 3.5)))));

        GeometryCollection geometryCollection = new GeometryCollection(of(point, lineString, polygonWithHoles, multiPoint, multiLineString,
            multiPolygon));
        
        AllTheThings allTheThings = new AllTheThings(name, geometryCollection);

        // when
        getDs().save(allTheThings);

        // then use the underlying driver to ensure it was persisted correctly to the database
        Document storedArea = getDatabase().getCollection(AllTheThings.class.getSimpleName()).find(new Document("name", name))
                                     .projection(new Document("_id", 0).append("className", 0))
                                     .first();
        assertThat(storedArea, is(notNullValue()));
        assertThat(storedArea.toString(), JSONMatcher.jsonEqual("  {"
                                                                + " name: '" + name + "',"
                                                                + " everything: "
                                                                + " {"
                                                                + "  type: 'GeometryCollection', "
                                                                + "  geometries: "
                                                                + "  ["
                                                                + "    {"
                                                                + "     type: 'Point', "
                                                                + "     coordinates: [7.0, 3.0]"
                                                                + "    }, "
                                                                + "    {"
                                                                + "     type: 'LineString', "
                                                                + "     coordinates: [ [ 2.0,  1.0],"
                                                                + "                    [ 5.0,  3.0],"
                                                                + "                    [13.0, 19.0] ]"
                                                                + "    },"
                                                                + "    {"
                                                                + "     type: 'Polygon', "
                                                                + "     coordinates: "
                                                                + "       [ [ [ 2.0, 1.1],"
                                                                + "           [ 3.5, 2.3],"
                                                                + "           [ 1.0, 3.7],"
                                                                + "           [ 2.0, 1.1] "
                                                                + "         ],"
                                                                + "         [ [ 2.0, 1.5],"
                                                                + "           [ 2.0, 1.9],"
                                                                + "           [ 1.8, 1.9],"
                                                                + "           [ 2.0, 1.5] "
                                                                + "         ],"
                                                                + "         [ [ 2.1, 2.2],"
                                                                + "           [ 1.9, 2.4],"
                                                                + "           [ 1.7, 2.4],"
                                                                + "           [ 1.8, 2.1],"
                                                                + "           [ 2.1, 2.2] "
                                                                + "         ]"
                                                                + "       ]"
                                                                + "    },"
                                                                + "    {"
                                                                + "     type: 'MultiPoint', "
                                                                + "     coordinates: [ [ 2.0,  1.0],"
                                                                + "                    [ 5.0,  3.0],"
                                                                + "                    [13.0, 19.0] ]"
                                                                + "    },"
                                                                + "    {"
                                                                + "     type: 'MultiLineString', "
                                                                + "     coordinates: "
                                                                + "        [ [ [ 2.0,  1.0],"
                                                                + "            [ 5.0,  3.0],"
                                                                + "            [13.0, 19.0] "
                                                                + "          ], "
                                                                + "          [ [ 2.0, 1.5],"
                                                                + "            [ 2.0, 1.9],"
                                                                + "            [ 1.8, 1.9],"
                                                                + "            [ 2.0, 1.5] "
                                                                + "          ]"
                                                                + "        ]"
                                                                + "    },"
                                                                + "    {"
                                                                + "     type: 'MultiPolygon', "
                                                                + "     coordinates: [ [ [ [ 2.0, 1.1],"
                                                                + "                        [ 3.5, 2.3],"
                                                                + "                        [ 1.0, 3.7],"
                                                                + "                        [ 2.0, 1.1],"
                                                                + "                      ]"
                                                                + "                    ],"
                                                                + "                    [ [ [ 3.0, 1.2],"
                                                                + "                        [ 4.5, 2.5],"
                                                                + "                        [ 1.9, 6.7],"
                                                                + "                        [ 3.0, 1.2] "
                                                                + "                      ],"
                                                                + "                      [ [ 2.4, 3.5],"
                                                                + "                        [ 2.8, 1.7],"
                                                                + "                        [ 2.4, 3.5] "
                                                                + "                      ],"
                                                                + "                    ]"
                                                                + "                  ]"
                                                                + "    }"
                                                                + "  ]"
                                                                + " }"
                                                                + "}"));
    }

    @Test
    public void shouldSaveAnEntityWithALineStringGeoJsonType() {
        // given
        Route route = new Route("My Route", new LineString(
            of(
                new Position(1, 2),
                new Position(3, 5),
                new Position(19, 13))));

        // when
        getDs().save(route);

        // then use the underlying driver to ensure it was persisted correctly to the database
        Document storedRoute = getDatabase().getCollection(Route.class.getSimpleName()).find(new Document("name", "My Route"))
                                      .projection(new Document("_id", 0).append("className", 0))
                                      .first();
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
        Stores stores = new Stores(name, new MultiPoint(
            of(
                new Position(2.0, 1.0),
                new Position(5.0, 3.0),
                new Position(13.0, 19.0))));

        // when
        getDs().save(stores);

        // then use the underlying driver to ensure it was persisted correctly to the database
        Document storedObject = getDatabase().getCollection(Stores.class.getSimpleName()).find(new Document("name", name))
                                       .projection(new Document("_id", 0).append("className", 0))
                                       .first();
        assertThat(storedObject, is(notNullValue()));
        assertThat(storedObject.toString(), JSONMatcher.jsonEqual("  {"
                                                                  + " name: " + name + ","
                                                                  + " locations:  "
                                                                  + " {"
                                                                  + "  type: 'MultiPoint', "
                                                                  + "  coordinates: [ [ 2.0,  1.0],"
                                                                  + "                 [ 5.0,  3.0],"
                                                                  + "                 [13.0, 19.0] ]"
                                                                  + " }"
                                                                  + "}"));
    }

    @Test
    public void shouldSaveAnEntityWithALocationStoredAsAPoint() {
        // given
        City city = new City("New City", new Point(new Position(7.0, 3.0)));

        // when
        getDs().save(city);

        // then use the underlying driver to ensure it was persisted correctly to the database
        Document storedCity = getDatabase().getCollection(City.class.getSimpleName()).find(new Document("name", "New City"))
                                     .projection(new Document("_id", 0).append("className", 0))
                                     .first();
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
        Paths paths = new Paths(name, new MultiLineString(of(
            of(
                new Position(1.0, 2.0),
                new Position(3.0, 5.0),
                new Position(19.0, 13.0)),
            of(
                new Position(1.5, 2.0),
                new Position(1.9, 2.0),
                new Position(1.9, 1.8),
                new Position(1.5, 2.0)))));

        // when
        getDs().save(paths);

        // then use the underlying driver to ensure it was persisted correctly to the database
        Document storedPaths = getDatabase().getCollection(Paths.class.getSimpleName()).find(new Document("name", name))
                                      .projection(new Document("_id", 0).append("className", 0))
                                      .first();
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
        PolygonCoordinates
            polygonWithHoles = new PolygonCoordinates(
            of(
                new Position(1.1, 2.0),
                new Position(2.3, 3.5),
                new Position(3.7, 1.0),
                new Position(1.1, 2.0)),
            of(
                new Position(1.5, 2.0),
                new Position(1.9, 2.0),
                new Position(1.9, 1.8),
                new Position(1.5, 2.0)),
            of(
                new Position(2.2, 2.1),
                new Position(2.4, 1.9),
                new Position(2.4, 1.7),
                new Position(2.1, 1.8),
                new Position(2.2, 2.1)));

        Regions regions = new Regions(name, new MultiPolygon(of(
            new PolygonCoordinates(
                of(
                    new Position(1.1, 2.0),
                    new Position(2.3, 3.5),
                    new Position(3.7, 1.0),
                    new Position(1.1, 2.0))),
            polygonWithHoles)));

        // when
        getDs().save(regions);

        // then use the underlying driver to ensure it was persisted correctly to the database
        Document storedRegions = getDatabase().getCollection(Regions.class.getSimpleName()).find(new Document("name", name))
                                              .projection(new Document("_id", 0).append("className", 0))
                                              .first();
        assertThat(storedRegions, is(notNullValue()));
        assertThat(storedRegions.toString(), JSONMatcher.jsonEqual("  {"
                                                                   + " name: '" + name + "',"
                                                                   + " regions:  "
                                                                   + " {"
                                                                   + "  type: 'MultiPolygon', "
                                                                   + "  coordinates: [ [ [ [ 2.0, 1.1],"
                                                                   + "                     [ 3.5, 2.3],"
                                                                   + "                     [ 1.0, 3.7],"
                                                                   + "                     [ 2.0, 1.1],"
                                                                   + "                   ]"
                                                                   + "                 ],"
                                                                   + "                 [ [ [ 2.0, 1.1],"
                                                                   + "                     [ 3.5, 2.3],"
                                                                   + "                     [ 1.0, 3.7],"
                                                                   + "                     [ 2.0, 1.1] "
                                                                   + "                   ],"
                                                                   + "                   [ [ 2.0, 1.5],"
                                                                   + "                     [ 2.0, 1.9],"
                                                                   + "                     [ 1.8, 1.9],"
                                                                   + "                     [ 2.0, 1.5] "
                                                                   + "                   ],"
                                                                   + "                   [ [ 2.1, 2.2],"
                                                                   + "                     [ 1.9, 2.4],"
                                                                   + "                     [ 1.7, 2.4],"
                                                                   + "                     [ 1.8, 2.1],"
                                                                   + "                     [ 2.1, 2.2] "
                                                                   + "                   ]"
                                                                   + "                 ]"
                                                                   + "               ]"
                                                                   + " }"
                                                                   + "}"));
    }

    @Test
    public void shouldSaveAnEntityWithAPolygonContainingInteriorRings() {
        // given
        String polygonName = "A polygon with holes";
        Polygon polygonWithHoles = new Polygon(
            of(
                new Position(2.0, 1.1),
                new Position(3.5, 2.3),
                new Position(1.0, 3.7),
                new Position(2.0, 1.1)),
            of(
                new Position(2.0, 1.5),
                new Position(2.0, 1.9),
                new Position(1.8, 1.9),
                new Position(2.0, 1.5)),
            of(
                new Position(2.1, 2.2),
                new Position(1.9, 2.4),
                new Position(1.7, 2.4),
                new Position(1.8, 2.1),
                new Position(2.1, 2.2)));
        Area area = new Area(polygonName, polygonWithHoles);

        // when
        getDs().save(area);

        // then use the underlying driver to ensure it was persisted correctly to the database
        Document storedArea = getDatabase().getCollection(Area.class.getSimpleName()).find(new Document("name", polygonName))
                                     .projection(new Document("_id", 0)
                                                     .append("className", 0)
                                                     .append("area.className", 0))
                                     .first();
        assertThat(storedArea, is(notNullValue()));
        assertThat(storedArea.toString(), JSONMatcher.jsonEqual("  {"
                                                                + " name: " + polygonName + ","
                                                                + " area:  "
                                                                + " {"
                                                                + "  type: 'Polygon', "
                                                                + "  coordinates: "
                                                                + "    [ [ [ 2.0, 1.1],"
                                                                + "        [ 3.5, 2.3],"
                                                                + "        [ 1.0, 3.7],"
                                                                + "        [ 2.0, 1.1] "
                                                                + "      ],"
                                                                + "      [ [ 2.0, 1.5],"
                                                                + "        [ 2.0, 1.9],"
                                                                + "        [ 1.8, 1.9],"
                                                                + "        [ 2.0, 1.5] "
                                                                + "      ],"
                                                                + "      [ [ 2.1, 2.2],"
                                                                + "        [ 1.9, 2.4],"
                                                                + "        [ 1.7, 2.4],"
                                                                + "        [ 1.8, 2.1],"
                                                                + "        [ 2.1, 2.2] "
                                                                + "      ]"
                                                                + "    ]"
                                                                + " }"
                                                                + "}"));
    }

    @Test
    public void shouldSaveAnEntityWithAPolygonGeoJsonType() {
        // given
        Area area = new Area("The Area", new Polygon(
            of(
                new Position(2.0, 1.1),
                new Position(3.5, 2.3),
                new Position(1.0, 3.7),
                new Position(2.0, 1.1))));

        // when
        getDs().save(area);

        // then use the underlying driver to ensure it was persisted correctly to the database
        Document storedArea = getDatabase().getCollection(Area.class.getSimpleName()).find(new Document("name", "The Area"))
                                     .projection(new Document("_id", 0)
                                                     .append("className", 0)
                                                     .append("area.className", 0))
                                     .first();
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

        Document storedCity = getDatabase().getCollection(City.class.getSimpleName())
                                     .find(new Document("name", "New City"))
                                     .projection(new Document("_id", 0).append("className", 0))
                                     .first();
        assertThat(storedCity, is(notNullValue()));
        assertThat(storedCity.toString(), JSONMatcher.jsonEqual("{ name: 'New City'}"));
    }


    @Entity
    private static final class Paths {
        @Id
        private ObjectId id;
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
            if (!paths.equals(paths1.paths)) {
                return false;
            }

            return true;
        }


        @Override
        public String toString() {
            return "Paths{"
                   + "name='" + name + '\''
                   + ", paths=" + paths
                   + '}';
        }
    }

    @Entity
    private static final class AllTheThings {
        @Id
        private ObjectId id;
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
            if (!name.equals(that.name)) {
                return false;
            }

            return true;
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
}
