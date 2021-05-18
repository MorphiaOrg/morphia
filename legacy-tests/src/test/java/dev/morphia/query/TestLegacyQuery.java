package dev.morphia.query;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import dev.morphia.Key;
import dev.morphia.TestBase;
import dev.morphia.annotations.CappedAt;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.testmodel.Rectangle;
import dev.morphia.testmodel.User;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.singletonList;
import static org.bson.Document.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


@SuppressWarnings({"unchecked", "unused", "removal"})
public class TestLegacyQuery extends TestBase {
    public TestLegacyQuery() {
        super(MapperOptions.legacy().build());
    }

    @Test
    @Before
    public void testCheckQueryClass() {
        Assume.assumeTrue("Should be using a LegacyQuery.", getDs().find(User.class) instanceof LegacyQuery);
    }

    @Test
    public void testCommentsShowUpInLogs() {
        getDs().save(asList(new Pic("pic1"), new Pic("pic2"), new Pic("pic3"), new Pic("pic4")));

        getDatabase().runCommand(new Document("profile", 2));
        String expectedComment = "test comment";

        getDs().find(Pic.class)
               .execute(new FindOptions()
                            .comment(expectedComment))
               .toList();

        MongoCollection<Document> profileCollection = getDatabase().getCollection("system.profile");
        assertNotEquals(0, profileCollection.countDocuments());

        Document query = new Document("op", "query")
                             .append("ns", getMapper().getCollection(Pic.class).getNamespace().getFullName())
                             .append("command.comment", new Document("$exists", true));
        Document profileRecord = profileCollection.find(query).first();

        assertEquals(profileRecord.toString(), expectedComment, getCommentFromProfileRecord(profileRecord));
    }

    @Test
    public void testComplexElemMatchQuery() {
        Keyword oscar = new Keyword("Oscar", 42);
        getDs().save(new PhotoWithKeywords(oscar, new Keyword("Jim", 12)));
        assertNull(getDs().find(PhotoWithKeywords.class)
                          .field("keywords")
                          .elemMatch(getDs()
                                         .find(Keyword.class)
                                         .filter("keyword = ", "Oscar")
                                         .filter("score = ", 12))
                          .execute(new FindOptions().limit(1))
                          .tryNext());

        List<PhotoWithKeywords> keywords = getDs().find(PhotoWithKeywords.class)
                                                  .field("keywords")
                                                  .elemMatch(getDs()
                                                                 .find(Keyword.class)
                                                                 .filter("score > ", 20)
                                                                 .filter("score < ", 100))
                                                  .execute().toList();
        assertEquals(1, keywords.size());
        assertEquals(oscar, keywords.get(0).keywords.get(0));
    }

    @Test
    public void testComplexRangeQuery() {
        getDs().save(asList(new Rectangle(1, 10), new Rectangle(4, 2), new Rectangle(6, 10), new Rectangle(8, 5), new Rectangle(10, 4)));

        assertEquals(2, getDs().find(Rectangle.class)
                               .filter("height >", 3)
                               .filter("height <", 8)
                               .count());
        assertEquals(1, getDs().find(Rectangle.class)
                               .filter("height >", 3)
                               .filter("height <", 8)
                               .filter("width", 10)
                               .count());
    }

    @Test
    public void testCriteriaContainers() {
        try {
            check(new DefaultQueryFactory().createQuery(getDs(), User.class).disableValidation());
            fail("These operations are not supported on the modern query operation and should have failed.");
        } catch (UnsupportedOperationException e) {
            // success
        }
        check(new LegacyQueryFactory().createQuery(getDs(), User.class).disableValidation());
    }

    @Test
    public void testElemMatchQuery() {
        getDs().save(asList(new PhotoWithKeywords(), new PhotoWithKeywords("Scott", "Joe", "Sarah")));
        assertNotNull(getDs().find(PhotoWithKeywords.class)
                             .field("keywords").elemMatch(getDs().find(Keyword.class).filter("keyword", "Scott"))
                             .execute(new FindOptions().limit(1))
                             .tryNext());
        assertNull(getDs().find(PhotoWithKeywords.class)
                          .field("keywords").elemMatch(getDs().find(Keyword.class).filter("keyword", "Randy"))
                          .execute(new FindOptions().limit(1))
                          .tryNext());
    }

    @Test
    public void testElemMatchVariants() {
        final PhotoWithKeywords pwk1 = new PhotoWithKeywords();
        final PhotoWithKeywords pwk2 = new PhotoWithKeywords("Kevin");
        final PhotoWithKeywords pwk3 = new PhotoWithKeywords("Scott", "Joe", "Sarah");
        final PhotoWithKeywords pwk4 = new PhotoWithKeywords(new Keyword("Scott", 14));

        Iterator<PhotoWithKeywords> iterator = getDs().save(asList(pwk1, pwk2, pwk3, pwk4)).iterator();
        Key<PhotoWithKeywords> key1 = getMapper().getKey(iterator.next());
        Key<PhotoWithKeywords> key2 = getMapper().getKey(iterator.next());
        Key<PhotoWithKeywords> key3 = getMapper().getKey(iterator.next());
        Key<PhotoWithKeywords> key4 = getMapper().getKey(iterator.next());

        assertListEquals(asList(key3, key4), getDs().find(PhotoWithKeywords.class)
                                                    .field("keywords")
                                                    .elemMatch(getDs().find(Keyword.class)
                                                                      .filter("keyword = ", "Scott"))
                                                    .keys());

        assertListEquals(asList(key3, key4), getDs().find(PhotoWithKeywords.class)
                                                    .field("keywords")
                                                    .elemMatch(getDs()
                                                                   .find(Keyword.class)
                                                                   .field("keyword").equal("Scott"))
                                                    .keys());

        assertListEquals(singletonList(key4), getDs().find(PhotoWithKeywords.class)
                                                     .field("keywords")
                                                     .elemMatch(getDs().find(Keyword.class)
                                                                       .filter("score = ", 14))
                                                     .keys());

        assertListEquals(singletonList(key4), getDs().find(PhotoWithKeywords.class)
                                                     .field("keywords")
                                                     .elemMatch(getDs()
                                                                    .find(Keyword.class)
                                                                    .field("score").equal(14))
                                                     .keys());

        assertListEquals(asList(key1, key2), getDs().find(PhotoWithKeywords.class)
                                                    .field("keywords")
                                                    .not()
                                                    .elemMatch(getDs().find(Keyword.class)
                                                                      .filter("keyword = ", "Scott"))
                                                    .keys());

        assertListEquals(asList(key1, key2), getDs().find(PhotoWithKeywords.class)
                                                    .field("keywords").not()
                                                    .elemMatch(getDs()
                                                                   .find(Keyword.class)
                                                                   .field("keyword").equal("Scott"))
                                                    .keys());
    }

    @Test
    public void testThatElemMatchQueriesOnlyChecksRequiredFields() {
        final PhotoWithKeywords pwk1 = new PhotoWithKeywords(new Keyword("california"), new Keyword("nevada"), new Keyword("arizona"));
        final PhotoWithKeywords pwk2 = new PhotoWithKeywords("Joe", "Sarah");
        pwk2.keywords.add(new Keyword("Scott", 14));

        getDs().save(asList(pwk1, pwk2));

        // In this case, we only want to match on the keyword field, not the
        // score field, which shouldn't be included in the elemMatch query.

        // As a result, the query in MongoDB should look like:
        // find({ keywords: { $elemMatch: { keyword: "Scott" } } })

        // NOT:
        // find({ keywords: { $elemMatch: { keyword: "Scott", score: 12 } } })
        assertNotNull(getDs().find(PhotoWithKeywords.class)
                             .field("keywords").elemMatch(getDs().find(Keyword.class)
                                                                 .filter("keyword", "Scott"))
                             .execute(new FindOptions().limit(1))
                             .tryNext());

        assertNull(getDs().find(PhotoWithKeywords.class)
                          .field("keywords").elemMatch(getDs().find(Keyword.class)
                                                              .filter("keyword", "Randy"))
                          .execute(new FindOptions().limit(1))
                          .tryNext());
    }

    private <T> void assertListEquals(List<Key<T>> list, MongoCursor<?> cursor) {
        for (Key<T> tKey : list) {
            assertEquals(list.toString(), tKey, cursor.next());
        }
    }

    private void check(Query<User> query) {
        query
            .field("version").equal("latest")
            .and(
                query.or(
                    query.criteria("fieldA").equal("a"),
                    query.criteria("fieldB").equal("b")),
                query.and(
                    query.criteria("fieldC").equal("c"),
                    query.or(
                        query.criteria("fieldD").equal("d"),
                        query.criteria("fieldE").equal("e"))));

        query.and(query.criteria("fieldF").equal("f"));

        final Document queryObject = query instanceof LegacyQuery
                                     ? query.toDocument()
                                     : query.toDocument();

        final Document parse = parse(
            "{\"version\": \"latest\", \"$and\": [{\"$or\": [{\"fieldA\": \"a\"}, {\"fieldB\": \"b\"}]}, {\"fieldC\": \"c\", \"$or\": "
            + "[{\"fieldD\": \"d\"}, {\"fieldE\": \"e\"}]}], \"fieldF\": \"f\","
            + "\"_t\": { \"$in\" : [ \"User\"]}}");

        Assert.assertEquals(parse, queryObject);
    }

    private int[] copy(int[] array, int start, int count) {
        return copyOfRange(array, start, start + count);
    }

    private void dropProfileCollection() {
        MongoCollection<Document> profileCollection = getDatabase().getCollection("system.profile");
        profileCollection.drop();
    }

    private String getCommentFromProfileRecord(Document profileRecord) {
        if (profileRecord.containsKey("command")) {
            Document commandDocument = ((Document) profileRecord.get("command"));
            if (commandDocument.containsKey("comment")) {
                return (String) commandDocument.get("comment");
            }
        }
        if (profileRecord.containsKey("query")) {
            Document queryDocument = ((Document) profileRecord.get("query"));
            if (queryDocument.containsKey("comment")) {
                return (String) queryDocument.get("comment");
            } else if (queryDocument.containsKey("$comment")) {
                return (String) queryDocument.get("$comment");
            }
        }
        return null;
    }

    private Query<Pic> getQuery(QueryFactory queryFactory) {
        return queryFactory.createQuery(getDs(), Pic.class);
    }

    private void turnOffProfiling() {
        getDatabase().runCommand(new Document("profile", 0).append("slowms", 100));
    }

    private void turnOnProfiling() {
        getDatabase().runCommand(new Document("profile", 2).append("slowms", 0));
    }

    @Entity(value = "capped_pic", cap = @CappedAt(count = 1000))
    public static class CappedPic extends Pic {
        public CappedPic() {
            super(System.currentTimeMillis() + "");
        }
    }

    @Entity(value = "user", useDiscriminator = false)
    private static class Class1 {
        @Id
        private ObjectId id;

        private String value1;

    }

    @Entity
    public static class ContainsPic {
        @Id
        private ObjectId id;
        private String name = "test";
        @Reference
        private Pic pic;
        @Reference(lazy = true)
        private Pic lazyPic;
        @Reference(lazy = true)
        private PicWithObjectId lazyObjectIdPic;
        @Indexed
        private int size;

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public PicWithObjectId getLazyObjectIdPic() {
            return lazyObjectIdPic;
        }

        public void setLazyObjectIdPic(PicWithObjectId lazyObjectIdPic) {
            this.lazyObjectIdPic = lazyObjectIdPic;
        }

        public Pic getLazyPic() {
            return lazyPic;
        }

        public void setLazyPic(Pic lazyPic) {
            this.lazyPic = lazyPic;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Pic getPic() {
            return pic;
        }

        public void setPic(Pic pic) {
            this.pic = pic;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        @Override
        public String toString() {
            return "ContainsPic{" +
                   "id=" + id +
                   ", name='" + name + '\'' +
                   ", size=" + size +
                   '}';
        }
    }

    @Entity(useDiscriminator = false)
    public static class ContainsRenamedFields {
        @Id
        private ObjectId id;
        @Property("first_name")
        private String firstName;
        @Property("last_name")
        private String lastName;

        public ContainsRenamedFields() {
        }

        ContainsRenamedFields(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

    @Entity("facebook_users")
    public static class FacebookUser {
        @Reference
        private final List<FacebookUser> friends = new ArrayList<>();
        public int loginCount;
        @Id
        private long id;
        private String username;

        public FacebookUser(long id, String name) {
            this();
            this.id = id;
            username = name;
        }

        public FacebookUser() {
        }

        public long getId() {
            return id;
        }
    }

    @Entity
    private static class GenericKeyValue<T> {

        @Id
        private ObjectId id;

        @Indexed(options = @IndexOptions(unique = true))
        private List<Object> key;

        private T value;
    }

    @Entity
    public static class HasIntId {
        @Id
        private int id;

        protected HasIntId() {
        }

        HasIntId(int id) {
            this.id = id;
        }
    }

    @Entity
    private static class HasPhotoReference {
        @Id
        private ObjectId id;
        @Reference
        private Photo photo;
    }

    @Entity
    static class IntVector {
        @Id
        private ObjectId id;
        private String name;
        private int[] scalars;

        IntVector() {
        }

        IntVector(int... scalars) {
            this.scalars = scalars;
        }
    }

    @Entity
    private static class KeyValue {
        @Id
        private ObjectId id;
        /**
         * The list of keys for this value.
         */
        @Indexed(options = @IndexOptions(unique = true))
        private List<Object> key;
        /**
         * The id of the value document
         */
        @Indexed
        private ObjectId value;
    }

    @Entity
    @SuppressWarnings({"UnusedDeclaration", "removal"})
    public static class Keys {
        @Id
        private ObjectId id;
        private List<Key<FacebookUser>> users;
        private Key<Rectangle> rect;

        private Keys() {
        }

        public Keys(Key<Rectangle> rectKey, List<Key<FacebookUser>> users) {
            rect = rectKey;
            this.users = users;
        }

        public ObjectId getId() {
            return id;
        }

        public Key<Rectangle> getRect() {
            return rect;
        }

        public List<Key<FacebookUser>> getUsers() {
            return users;
        }
    }

    @Entity
    public static class Keyword {
        private String keyword;
        private Integer score;

        protected Keyword() {
        }

        Keyword(String k) {
            this.keyword = k;
        }

        Keyword(String k, Integer score) {
            this.keyword = k;
            this.score = score;
        }

        Keyword(Integer score) {
            this.score = score;
        }

        @Override
        public int hashCode() {
            int result = keyword != null ? keyword.hashCode() : 0;
            result = 31 * result + (score != null ? score.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Keyword)) {
                return false;
            }

            final Keyword keyword1 = (Keyword) o;

            if (keyword != null ? !keyword.equals(keyword1.keyword) : keyword1.keyword != null) {
                return false;
            }
            return score != null ? score.equals(keyword1.score) : keyword1.score == null;

        }

    }

    @Entity
    public static class Photo {
        @Id
        private ObjectId id;
        private List<String> keywords = singletonList("amazing");

        public Photo() {
        }

        Photo(List<String> keywords) {
            this.keywords = keywords;
        }
    }

    @Entity
    public static class PhotoWithKeywords {
        @Id
        private ObjectId id;
        private List<Keyword> keywords = new ArrayList<>();

        PhotoWithKeywords() {
        }

        PhotoWithKeywords(String... words) {
            keywords = new ArrayList<>(words.length);
            for (String word : words) {
                keywords.add(new Keyword(word));
            }
        }

        PhotoWithKeywords(Keyword... keyword) {
            keywords.addAll(asList(keyword));
        }
    }

    @Entity
    public static class Pic {
        @Id
        private ObjectId id;
        @Indexed
        private String name;
        private boolean prePersist;

        public Pic() {
        }

        Pic(String name) {
            this.name = name;
        }

        public ObjectId getId() {
            return id;
        }

        public void setId(ObjectId id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            int result = getId() != null ? getId().hashCode() : 0;
            result = 31 * result + (getName() != null ? getName().hashCode() : 0);
            result = 31 * result + (isPrePersist() ? 1 : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pic)) {
                return false;
            }

            final Pic pic = (Pic) o;

            if (isPrePersist() != pic.isPrePersist()) {
                return false;
            }
            if (getId() != null ? !getId().equals(pic.getId()) : pic.getId() != null) {
                return false;
            }
            return getName() != null ? getName().equals(pic.getName()) : pic.getName() == null;
        }

        @PrePersist
        public void tweak() {
            prePersist = true;
        }

        boolean isPrePersist() {
            return prePersist;
        }

        public void setPrePersist(boolean prePersist) {
            this.prePersist = prePersist;
        }
    }

    @Entity
    public static class PicWithObjectId {
        @Id
        private ObjectId id;
        private String name;
    }

    private static class RectangleComparator implements Comparator<Rectangle> {
        @Override
        public int compare(Rectangle o1, Rectangle o2) {
            int compare = Double.compare(o1.getWidth(), o2.getWidth());
            return compare != 0 ? compare : Double.compare(o2.getHeight(), o1.getHeight());
        }
    }

    private static class RectangleComparator1 implements Comparator<Rectangle> {
        @Override
        public int compare(Rectangle o1, Rectangle o2) {
            int compare = Double.compare(o2.getHeight(), o1.getHeight());
            return compare != 0 ? compare : Double.compare(o2.getWidth(), o1.getWidth());
        }
    }

    private static class RectangleComparator2 implements Comparator<Rectangle> {
        @Override
        public int compare(Rectangle o1, Rectangle o2) {
            int compare = Double.compare(o1.getWidth(), o2.getWidth());
            return compare != 0 ? compare : Double.compare(o1.getHeight(), o2.getHeight());
        }
    }

    private static class RectangleComparator3 implements Comparator<Rectangle> {
        @Override
        public int compare(Rectangle o1, Rectangle o2) {
            int compare = Double.compare(o1.getWidth(), o2.getWidth());
            return compare != 0 ? compare : Double.compare(o1.getHeight(), o2.getHeight());
        }
    }

    @Entity
    static class ReferenceKey {
        @Id
        private ObjectId id;
        private String name;

        ReferenceKey() {
        }

        ReferenceKey(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ReferenceKey that = (ReferenceKey) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            return name != null ? name.equals(that.name) : that.name == null;
        }
    }

    @Entity
    private static class ReferenceKeyValue {
        @Id
        private ReferenceKey id;
        /**
         * The list of keys for this value.
         */
        @Indexed(options = @IndexOptions(unique = true))
        @Reference
        private List<Pic> key;
        /**
         * The id of the value document
         */
        @Indexed
        private ObjectId value;
    }
}
