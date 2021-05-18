package dev.morphia.test.indexes;

import com.mongodb.MongoCommandException;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import dev.morphia.Datastore;
import dev.morphia.annotations.Collation;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Text;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.methods.MethodMappedUser;
import dev.morphia.utils.IndexDirection;
import dev.morphia.utils.IndexType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.CollationAlternate.SHIFTED;
import static dev.morphia.test.util.IndexMatcher.doesNotHaveIndexNamed;
import static dev.morphia.test.util.IndexMatcher.hasIndexNamed;
import static dev.morphia.utils.IndexType.DESC;
import static dev.morphia.utils.IndexType.TEXT;
import static org.bson.Document.parse;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestIndexes extends TestBase {
    @Test
    public void indexTypefromValue() {
        assertEquals(IndexType.fromValue(1), IndexType.ASC);
        assertEquals(IndexType.fromValue(-1), IndexType.DESC);
        assertEquals(IndexType.fromValue("2d"), IndexType.GEO2D);
        assertEquals(IndexType.fromValue("2dsphere"), IndexType.GEO2DSPHERE);
        assertEquals(IndexType.fromValue("hashed"), IndexType.HASHED);
        assertEquals(IndexType.fromValue("text"), IndexType.TEXT);
    }

    @Test
    public void mutipleUniqueIndexed() {
        getMapper().map(UniqueIndexOnValue.class);
        getDs().ensureIndexes();
        long value = 7L;

        try {
            final UniqueIndexOnValue entityWithUniqueName = new UniqueIndexOnValue();
            entityWithUniqueName.setValue(value);
            entityWithUniqueName.setUnique(1);
            getDs().save(entityWithUniqueName);

            final UniqueIndexOnValue entityWithSameName = new UniqueIndexOnValue();
            entityWithSameName.setValue(value);
            entityWithSameName.setUnique(2);
            getDs().save(entityWithSameName);

            Assert.fail("Should have gotten a duplicate key exception");
        } catch (Exception ignored) {
        }

        value = 10L;
        try {
            final UniqueIndexOnValue first = new UniqueIndexOnValue();
            first.setValue(1);
            first.setUnique(value);
            getDs().save(first);

            final UniqueIndexOnValue second = new UniqueIndexOnValue();
            second.setValue(2);
            second.setUnique(value);
            getDs().save(second);

            Assert.fail("Should have gotten a duplicate key exception");
        } catch (Exception ignored) {
        }
    }

    @Test(expectedExceptions = MongoCommandException.class)
    public void shouldNotAllowMultipleTextIndexes() {
        getMapper().map(MultipleTextIndexes.class);
        getDs().ensureIndexes();
    }

    @Test
    public void testCanCreate2dSphereIndexes() {
        // given
        getMapper().map(Place.class);

        // when
        getDs().ensureIndexes(Place.class);

        // then
        List<Document> indexInfo = getIndexInfo(Place.class);
        assertThat(indexInfo.size(), is(2));
        assertThat(indexInfo, hasIndexNamed("location_2dsphere"));
    }

    @Test
    public void testCanCreate2dSphereIndexesOnLegacyCoordinatePairs() {
        // given
        getMapper().map(LegacyPlace.class);

        // when
        getDs().ensureIndexes(LegacyPlace.class);

        // then
        List<Document> indexInfo = getIndexInfo(LegacyPlace.class);
        assertThat(indexInfo, hasIndexNamed("location_2dsphere"));
    }

    @Test
    public void testClassIndexInherit() {
        getMapper().map(Shape.class, Circle.class);
        final EntityModel entityModel = getMapper().getEntityModel(Circle.class);
        assertNotNull(entityModel);

        assertNotNull(entityModel.getAnnotation(Indexes.class));

        getDs().ensureIndexes();

        assertEquals(getIndexInfo(Circle.class).size(), 4);
    }

    @Test
    public void testExpireAfterClassAnnotation() {
        getMapper().map(ClassAnnotation.class);
        getDs().ensureIndexes(ClassAnnotation.class);

        getDs().save(new ClassAnnotation());

        final List<Document> indexes = getIndexInfo(ClassAnnotation.class);
        assertEquals(indexes.size(), 2);
        Document index = null;
        for (Document candidateIndex : indexes) {
            if (candidateIndex.containsKey("expireAfterSeconds")) {
                index = candidateIndex;
            }
        }
        assertNotNull(index);
        assertTrue(index.containsKey("expireAfterSeconds"));
        assertEquals(((Number) index.get("expireAfterSeconds")).intValue(), 5);
    }

    @Test
    public void testIndexedField() {
        getMapper().map(HasExpiryField.class);
        getDs().ensureIndexes(HasExpiryField.class);

        getDs().save(new HasExpiryField());

        final List<Document> indexes = getIndexInfo(HasExpiryField.class);

        assertNotNull(indexes);
        assertEquals(indexes.size(), 2);
        Document index = null;
        for (Document candidateIndex : indexes) {
            if (candidateIndex.containsKey("expireAfterSeconds")) {
                index = candidateIndex;
            }
        }
        assertNotNull(index);
        assertEquals(((Number) index.get("expireAfterSeconds")).intValue(), 5);
    }

    @Test
    public void testIndexedRecursiveEntity() {
        getMapper().getEntityModel(CircularEmbeddedEntity.class);
        getMapper().getCollection(CircularEmbeddedEntity.class).drop();
        getDs().ensureIndexes(CircularEmbeddedEntity.class);
        assertThat(getIndexInfo(CircularEmbeddedEntity.class), hasIndexNamed("a_1"));
    }

    @Test
    public void testIndexes() {
        final Datastore datastore = getDs();
        datastore.ensureIndexes(TestWithIndexOption.class);
        List<Document> indexInfo = getIndexInfo(TestWithIndexOption.class);
        assertEquals(indexInfo.size(), 2);
        assertBackground(indexInfo);
        for (Document document : indexInfo) {
            if (document.get("name").equals("collated")) {
                assertEquals(document.get("partialFilterExpression"),
                    parse("{ name : { $exists : true } }"));
                Document collation = (Document) document.get("collation");
                collation.remove("version");

                Document parse = parse("{ 'locale': 'en_US', "
                                       + "'alternate': 'shifted',"
                                       + "'backwards': true,"
                                       + "'caseFirst': 'upper',"
                                       + "'caseLevel': true,"
                                       + "'maxVariable': 'space',"
                                       + "'normalization': true,"
                                       + "'numericOrdering': true,"
                                       + "'strength': 5 }");
                assertEquals(collation, parse, collation.toJson());
            }
        }

        datastore.ensureIndexes(TestWithDeprecatedIndex.class);
        assertEquals(getIndexInfo(TestWithDeprecatedIndex.class).size(), 2);
        assertBackground(getIndexInfo(TestWithDeprecatedIndex.class));

        datastore.ensureIndexes(TestWithHashedIndex.class);
        assertEquals(getIndexInfo(TestWithHashedIndex.class).size(), 2);
        assertHashed(getIndexInfo(TestWithHashedIndex.class));
    }

    @Test
    public void testInheritedFieldIndex() {
        getMapper().map(Shape.class, Circle.class);
        getDs().ensureIndexes();
        assertEquals(getIndexInfo(Circle.class).size(), 4);
    }

    @Test
    public void testMethodMapping() {
        withOptions(MapperOptions.builder().propertyDiscovery(PropertyDiscovery.METHODS).build(),
            () -> {
                getMapper().map(MethodMappedUser.class);
                getDs().ensureIndexes(MethodMappedUser.class);
                assertEquals(getIndexInfo(MethodMappedUser.class).size(), 3);
            });
    }

    @Test
    public void testMixedIndexes() {
        getMapper().getEntityModel(Ad2.class);

        assertThat(getIndexInfo(Ad2.class), doesNotHaveIndexNamed("active_1_lastMod_-1"));
        getDs().ensureIndexes(Ad2.class);
        assertThat(getIndexInfo(Ad2.class), hasIndexNamed("active_1_lastMod_-1"));
        assertThat(getIndexInfo(Ad2.class), hasIndexNamed("lastMod_1"));
    }

    @Test
    public void testNamedIndexEntity() {
        getDs().getMapper().map(NamedIndexOnValue.class);
        getDs().ensureIndexes(NamedIndexOnValue.class);

        assertThat(getIndexInfo(NamedIndexOnValue.class), hasIndexNamed("value_ascending"));
    }

    @Test
    public void testSingleAnnotation() {
        getMapper().map(CompoundTextIndex.class);
        getDs().ensureIndexes();

        List<Document> indexInfo = getIndexInfo(CompoundTextIndex.class);
        Assert.assertEquals(indexInfo.size(), 2);
        boolean found = false;
        for (Document document : indexInfo) {
            if (document.get("name").equals("indexing_test")) {
                found = true;
                Assert.assertEquals(document.get("default_language"), "russian", document.toString());
                Assert.assertEquals(document.get("language_override"), "nativeTongue", document.toString());
                Assert.assertEquals(((Document) document.get("weights")).get("name"), 1, document.toString());
                Assert.assertEquals(((Document) document.get("weights")).get("nick"), 10, document.toString());
                Assert.assertEquals(((Document) document.get("key")).get("age"), 1, document.toString());
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void testTextAnnotation() {
        Class<SingleFieldTextIndex> clazz = SingleFieldTextIndex.class;

        getMapper().map(clazz);
        getDs().ensureIndexes();

        List<Document> indexInfo = getIndexInfo(clazz);
        Assert.assertEquals(indexInfo.size(), 2, indexInfo.toString());
        boolean found = false;
        for (Document document : indexInfo) {
            if (document.get("name").equals("single_annotation")) {
                found = true;
                Assert.assertEquals(document.get("default_language"), "english", document.toString());
                Assert.assertEquals(document.get("language_override"), "nativeTongue", document.toString());
                Assert.assertEquals(((Document) document.get("weights")).get("nickName"), 10, document.toString());
            }
        }
        Assert.assertTrue(found, indexInfo.toString());

    }

    private void assertBackground(List<Document> indexInfo) {
        for (Document document : indexInfo) {
            if (!document.getString("name").equals("_id_")) {
                assertTrue(document.getBoolean("background"));
            }
        }
    }

    private void assertHashed(List<Document> indexInfo) {
        for (Document document : indexInfo) {
            if (!document.getString("name").equals("_id_")) {
                assertEquals(((Document) document.get("key")).get("hashedValue"), "hashed");
            }
        }
    }

    @Entity
    @Indexes(@Index(fields = {@Field("active"), @Field(value = "lastModified", type = IndexType.DESC)},
        options = @IndexOptions(unique = true)))
    private static class Ad2 {
        @Id
        private long id;

        @Indexed
        @Property("lastMod")
        private long lastModified;

        @Indexed
        private boolean active;
    }

    @Indexes(@Index(fields = @Field("radius")))
    private static class Circle extends Shape {
        private final double radius = 1;

        Circle() {
            description = "Circles are round and can be rolled along the ground.";
        }
    }

    @Entity
    private static class CircularEmbeddedEntity {
        @Id
        private final ObjectId id = new ObjectId();
        private String name;
        @Indexed
        private CircularEmbeddedEntity a;
    }

    @Entity
    @Indexes(@Index(fields = @Field("offerExpiresAt"), options = @IndexOptions(expireAfterSeconds = 5)))
    private static class ClassAnnotation {
        private final Date offerExpiresAt = new Date();
        @Id
        private ObjectId id;
    }

    @Entity
    @Indexes(@Index(fields = {@Field(value = "name", type = TEXT),
                              @Field(value = "nick", type = TEXT, weight = 10),
                              @Field(value = "age")}, options = @IndexOptions(name = "indexing_test", language = "russian",
        languageOverride = "nativeTongue")))
    private static class CompoundTextIndex {
        @Id
        private ObjectId id;
        private String name;
        private Integer age;
        @Property("nick")
        private String nickName;
        private String nativeTongue;
    }

    @Entity
    @Indexes(@Index(fields = {@Field("actor.actorObject.userId"), @Field(value = "actor.actorType", type = DESC)},
        options = @IndexOptions(disableValidation = true,
            partialFilter = "{ 'actor.actorObject.userId': { $exists: true }, 'actor.actorType': { $exists: true } }")))
    private static class FeedEvent {
        @Id
        private ObjectId id;
    }

    @Entity
    private static class HasExpiryField {
        @Indexed(options = @IndexOptions(expireAfterSeconds = 5))
        private final Date offerExpiresAt = new Date();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class InboxEvent {
        @Id
        private ObjectId id;
        private FeedEvent feedEvent;
    }

    @Entity
    private static class LegacyPlace {
        @Id
        private long id;

        @Indexed(IndexDirection.GEO2DSPHERE)
        private double[] location;
    }

    @Entity
    @Indexes({@Index(fields = @Field(value = "name", type = TEXT)),
              @Index(fields = @Field(value = "nickName", type = TEXT))})
    private static class MultipleTextIndexes {
        @Id
        private ObjectId id;
        private String name;
        private String nickName;
    }

    @Entity
    private static class NamedIndexOnValue {
        @Indexed(options = @IndexOptions(name = "value_ascending"))
        private final long value = 4;
        @Id
        private ObjectId id;
    }

    @Entity
    private static class Place {
        @Id
        private long id;

        @Indexed(IndexDirection.GEO2DSPHERE)
        private Object location;
    }

    @Entity
    @Indexes(@Index(fields = @Field("description")))
    public abstract static class Shape {
        @Id
        ObjectId id;
        String description;
        @Indexed
        String foo;
    }

    @Entity
    private static class SingleFieldTextIndex {
        @Id
        private ObjectId id;
        private String name;
        @Text(value = 10, options = @IndexOptions(name = "single_annotation", languageOverride = "nativeTongue"))
        private String nickName;

    }

    @Entity(useDiscriminator = false)
    @Indexes({@Index(options = @IndexOptions(background = true),
        fields = @Field("name"))})
    private static class TestWithDeprecatedIndex {
        @Id
        private ObjectId id;
        private String name;

    }

    @Entity(useDiscriminator = false)
    @Indexes({@Index(options = @IndexOptions(), fields = {@Field(value = "hashedValue", type = IndexType.HASHED)})})
    private static class TestWithHashedIndex {
        @Id
        private ObjectId id;
        private String hashedValue;

    }

    @Entity(useDiscriminator = false)
    @Indexes({@Index(options = @IndexOptions(name = "collated",
        partialFilter = "{ name : { $exists : true } }",
        collation = @Collation(locale = "en_US", alternate = SHIFTED, backwards = true,
            caseFirst = CollationCaseFirst.UPPER, caseLevel = true, maxVariable = CollationMaxVariable.SPACE, normalization = true,
            numericOrdering = true, strength = CollationStrength.IDENTICAL),
        background = true),
        fields = {@Field(value = "name")})})
    private static class TestWithIndexOption {
        @Id
        private ObjectId id;
        private String name;

    }

    @Entity
    private static class UniqueIndexOnValue {
        @Id
        private ObjectId id;

        @Indexed(options = @IndexOptions(name = "l_ascending", unique = true))
        private long value;

        @Indexed(options = @IndexOptions(unique = true))
        private long unique;

        private String name;

        public void setUnique(long value) {
            this.unique = value;
        }

        public void setValue(long value) {
            this.value = value;
        }
    }
}
