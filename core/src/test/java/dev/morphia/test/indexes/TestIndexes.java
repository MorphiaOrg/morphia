package dev.morphia.test.indexes;

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
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MapperOptions.PropertyDiscovery;
import dev.morphia.test.TestBase;
import dev.morphia.test.models.methods.MethodMappedUser;
import dev.morphia.utils.IndexType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.CollationAlternate.SHIFTED;
import static dev.morphia.utils.IndexType.DESC;
import static org.bson.Document.parse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class TestIndexes extends TestBase {

    @Test
    public void testExpireAfterClassAnnotation() {
        getMapper().map(ClassAnnotation.class);
        getDs().ensureIndexes();

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
        getDs().ensureIndexes();

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
    public void testMethodMapping() {
        withOptions(MapperOptions.builder().propertyDiscovery(PropertyDiscovery.METHODS).build(),
            () -> {
                getMapper().map(MethodMappedUser.class);
                getDs().ensureIndexes(MethodMappedUser.class);
                assertEquals(getIndexInfo(MethodMappedUser.class).size(), 3);
            });
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
    @Indexes(@Index(fields = @Field("offerExpiresAt"), options = @IndexOptions(expireAfterSeconds = 5)))
    private static class ClassAnnotation {
        private final Date offerExpiresAt = new Date();
        @Id
        private ObjectId id;
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

}
