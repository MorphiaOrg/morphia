package dev.morphia.test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;

import com.mongodb.client.result.UpdateResult;

import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.ModifyOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.PreLoad;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Operations;
import dev.morphia.query.Query;
import dev.morphia.query.ValidationException;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import dev.morphia.test.models.Circle;
import dev.morphia.test.models.FacebookUser;
import dev.morphia.test.models.Hotel;
import dev.morphia.test.models.Shape;
import dev.morphia.test.models.TestEntity;
import dev.morphia.test.models.generics.Child;
import dev.morphia.test.query.TestQuery.CappedPic;
import dev.morphia.test.query.TestQuery.ContainsPic;
import dev.morphia.test.query.TestQuery.Pic;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static dev.morphia.query.filters.Filters.eq;
import static dev.morphia.query.updates.UpdateOperators.addToSet;
import static dev.morphia.query.updates.UpdateOperators.and;
import static dev.morphia.query.updates.UpdateOperators.bit;
import static dev.morphia.query.updates.UpdateOperators.currentDate;
import static dev.morphia.query.updates.UpdateOperators.dec;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static dev.morphia.query.updates.UpdateOperators.max;
import static dev.morphia.query.updates.UpdateOperators.min;
import static dev.morphia.query.updates.UpdateOperators.mul;
import static dev.morphia.query.updates.UpdateOperators.or;
import static dev.morphia.query.updates.UpdateOperators.pop;
import static dev.morphia.query.updates.UpdateOperators.pull;
import static dev.morphia.query.updates.UpdateOperators.pullAll;
import static dev.morphia.query.updates.UpdateOperators.push;
import static dev.morphia.query.updates.UpdateOperators.rename;
import static dev.morphia.query.updates.UpdateOperators.set;
import static dev.morphia.query.updates.UpdateOperators.setOnInsert;
import static dev.morphia.query.updates.UpdateOperators.unset;
import static dev.morphia.query.updates.UpdateOperators.xor;
import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

@SuppressWarnings({ "ConstantConditions", "unused" })
public class TestUpdateOperations extends TestBase {

    private static final JsonWriterSettings WRITER_SETTINGS = JsonWriterSettings.builder().indent(true).build();

    public TestUpdateOperations() {
        super(buildConfig(LogHolder.class, CappedPic.class, Shape.class));
    }

    @Test
    public void retainsClassName() {
        final MapsOfStuff mapsOfStuff = new MapsOfStuff();

        final Stuff1 stuff1 = new Stuff1();
        stuff1.foo = "narf";
        mapsOfStuff.map.put("k1", stuff1);

        final Stuff2 stuff2 = new Stuff2();
        stuff2.bar = "blarg";
        mapsOfStuff.map.put("k2", stuff2);

        getDs().save(mapsOfStuff);

        final Query<MapsOfStuff> query = getDs().find(MapsOfStuff.class);
        query.update(set("map.k2", stuff1));

        // fails due to type now missing
        getDs().find(MapsOfStuff.class).iterator(new FindOptions().limit(1))
                .next();
    }

    @Test(description = "see https://github.com/MorphiaOrg/morphia/issues/2472 for details")
    public void testUpdateWithDocumentConversion() {
        getDs().find(Hotel.class).filter(eq("_id", ObjectId.get()))
                .disableValidation()
                .update(
                        set("last_updated", LocalDateTime.now()),
                        push("logs", List.of(Map.of("1", 1L))),
                        push("user_detail", List.of(new FacebookUser())));
    }

    @Test
    public void shouldUpdateAnArrayElement() {
        // given
        ObjectId parentId = new ObjectId();
        String childName = "Bob";
        String updatedLastName = "updatedLastName";

        Parent parent = new Parent();
        parent.id = parentId;
        parent.children.add(new Child("Anthony", "Child"));
        parent.children.add(new Child(childName, "originalLastName"));
        getDs().save(parent);

        // when
        Query<Parent> query = getDs().find(Parent.class)
                .filter(eq("_id", parentId),
                        eq("children.first", childName));
        UpdateResult updateResult = query.update(set("children.$.last", updatedLastName));

        // then
        assertThat(updateResult.getModifiedCount(), is(1L));
        assertThat(getDs().find(Parent.class)
                .filter(eq("id", parentId)).iterator(new FindOptions().limit(1))
                .next().children, hasItem(new Child(childName, updatedLastName)));
    }

    @Test
    public void testInsertWithRef() {
        final Pic pic = new Pic();
        pic.setName("fist");
        final ObjectId picKey = getDs().save(pic).getId();

        Query<ContainsPic> query = getDs().find(ContainsPic.class)
                .filter(eq("name", "first"),
                        eq("pic", picKey));
        assertInserted(query.update(new UpdateOptions().upsert(true), set("name", "A")));
        MatcherAssert.assertThat(getDs().find(ContainsPic.class).count(), is(1L));
        getDs().find(ContainsPic.class).delete(new DeleteOptions().multi(true));

        query = getDs().find(ContainsPic.class)
                .filter(eq("name", "first"),
                        eq("pic", pic));
        assertInserted(query.update(new UpdateOptions().upsert(true), set("name", "second")));
        MatcherAssert.assertThat(getDs().find(ContainsPic.class).count(), is(1L));

        //test reading the object.
        final ContainsPic cp = getDs().find(ContainsPic.class).first();
        assertThat(cp, is(notNullValue()));
        MatcherAssert.assertThat(cp.getName(), is("second"));
        MatcherAssert.assertThat(cp.getPic(), is(notNullValue()));
        MatcherAssert.assertThat(cp.getPic().getName(), is(notNullValue()));
        MatcherAssert.assertThat(cp.getPic().getName(), is("fist"));
    }

    @Test
    public void testInsertUpdate() {
        assertInserted(getDs().find(Circle.class)
                .filter(eq("radius", 0))
                .update(new UpdateOptions().upsert(true), inc("radius", 1D)));
    }

    @Test
    public void testInvalidPathsInUpdates() {
        Consumer<Datastore> test = (datastore) -> {
            Query<CappedPic> query = getDs().find(CappedPic.class);
            assertThrows(ValidationException.class, () -> query.update(max("bad.name", 12)));
            query.first();
        };

        test.accept(getDs());
        withConfig(buildConfig().legacy(), () -> {
            test.accept(getDs());
        });
    }

    @Test
    public void testInvalidPathsInModify() {
        Consumer<Datastore> test = (datastore) -> {
            Query<CappedPic> query = getDs().find(CappedPic.class);
            assertThrows(ValidationException.class, () -> query.modify(new ModifyOptions(), max("bad.name", 12)));
            query.first();
        };

        test.accept(getDs());
        withConfig(buildConfig().legacy(), () -> {
            test.accept(getDs());
        });
    }

    @Test
    public void testPolymorphicUpsert() {
        withConfig(buildConfig()
                .enablePolymorphicQueries(true), () -> {
                    final ObjectId id = new ObjectId();
                    final double originalValue = 2D;

                    Datastore ds = getDs();
                    Query<Circle> query = ds.find(Circle.class)
                            .filter(eq("id", id));
                    assertInserted(query.update(new UpdateOptions().upsert(true), setOnInsert(Map.of("radius", originalValue))));

                    Shape first = ds.find(Shape.class).first();
                    assertNotNull(first);
                    assertTrue(first instanceof Circle);
                    assertEquals(((Circle) first).getRadius(), originalValue);
                });
    }

    @Test
    public void testUpdateRef() {
        final ContainsPic cp = new ContainsPic();
        cp.setName("cp one");

        getDs().save(cp);

        final Pic pic = new Pic();
        pic.setName("fist");
        getDs().save(pic);

        Query<ContainsPic> query = getDs().find(ContainsPic.class)
                .filter(eq("name", cp.getName()));
        UpdateResult result = query.update(set("pic", pic));
        assertEquals(result.getModifiedCount(), 1);

        //test reading the object.
        final ContainsPic cp2 = getDs().find(ContainsPic.class).iterator(new FindOptions().limit(1))
                .next();
        assertThat(cp2, is(notNullValue()));
        MatcherAssert.assertThat(cp.getName(), CoreMatchers.is(cp2.getName()));
        MatcherAssert.assertThat(cp2.getPic(), is(notNullValue()));
        MatcherAssert.assertThat(cp2.getPic().getName(), is(notNullValue()));
        MatcherAssert.assertThat(pic.getName(), CoreMatchers.is(cp2.getPic().getName()));

        //test reading the object.
        final ContainsPic cp3 = getDs().find(ContainsPic.class).iterator(new FindOptions().limit(1))
                .next();
        assertThat(cp3, is(notNullValue()));
        MatcherAssert.assertThat(cp.getName(), CoreMatchers.is(cp3.getName()));
        MatcherAssert.assertThat(cp3.getPic(), is(notNullValue()));
        MatcherAssert.assertThat(cp3.getPic().getName(), is(notNullValue()));
        MatcherAssert.assertThat(pic.getName(), CoreMatchers.is(cp3.getPic().getName()));
    }

    @Test
    public void testUpdateWithDifferentType() {
        final ContainsInt cInt = new ContainsInt();
        cInt.val = 21;
        getDs().save(cInt);

        Query<ContainsInt> query = getDs().find(ContainsInt.class);

        final UpdateResult res = query.update(inc("val", 1.1D));
        assertUpdated(res);

        assertEquals(query.iterator(new FindOptions()
                .limit(1))
                .next().val, 22);
    }

    @Test(expectedExceptions = ValidationException.class)
    public void testValidationBadFieldName() {
        Query<Circle> query = getDs().find(Circle.class)
                .filter(eq("radius", 0));
        query.update(inc("rad", 1D));
    }

    @Test
    public void testValidation() {
        getDs().getCollection(Circle.class).drop();
        Query<Circle> query = getDs().find(Circle.class)
                .disableValidation()
                .filter(eq("radius", 0));
        query.update(new UpdateOptions().upsert(true),
                inc("rad", 1D));

        Document shapes = getDs().find("shapes", Document.class)
                .first();
        assertTrue(shapes.containsKey("rad"));
    }

    @Test
    public void testUpsert() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        ContainsIntArray control = new ContainsIntArray();
        getDs().save(asList(cIntArray, control));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);

        doUpdates(cIntArray, control, query.update(addToSet("values", 4)),
                new Integer[] { 1, 2, 3, 4 });

        doUpdates(cIntArray, control, query.update(addToSet("values", asList(4, 5))),
                new Integer[] { 1, 2, 3, 4, 5 });

        assertInserted(getDs().find(ContainsIntArray.class)
                .filter(eq("values", new Integer[] { 4, 5, 7 }))
                .update(new UpdateOptions().upsert(true), addToSet("values", 6)));

        query = getDs().find(ContainsIntArray.class)
                .filter(eq("values", new Integer[] { 4, 5, 7, 6 }));
        FindOptions options = new FindOptions()
                .logQuery();
        assertNotNull(query.first(options), query.getLoggedQuery());
    }

    private void assertInserted(UpdateResult res) {
        assertNotNull(res.getUpsertedId());
        assertEquals(res.getModifiedCount(), 0);
    }

    private void assertUpdated(UpdateResult res) {
        assertEquals(1, res.getModifiedCount());
    }

    private void doUpdates(ContainsIntArray updated, ContainsIntArray control, UpdateResult result, Integer[] target) {
        assertUpdated(result);
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", updated.id))
                .first().values,
                is(target));
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", control.id))
                .first().values,
                is(new Integer[] { 1, 2, 3 }));

        assertEquals(result.getMatchedCount(), 1);
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", updated.id))
                .first().values,
                is(target));
        assertThat(getDs().find(ContainsIntArray.class)
                .filter(eq("_id", control.id))
                .first().values,
                is(new Integer[] { 1, 2, 3 }));
    }

    private Integer[] get(ContainsIntArray array) {
        return getDs().find(ContainsIntArray.class)
                .filter(eq("_id", array.id))
                .first().values;
    }

    private enum TestEnum {
        ANYVAL,
        ANOTHERVAL
    }

    @Entity
    private static class ContainsInt {
        @Id
        private ObjectId id;
        private int val;
    }

    @Entity
    private static class ContainsIntArray {
        private final Integer[] values = { 1, 2, 3 };
        @Id
        private ObjectId id;
    }

    @Entity
    private static final class DumbArrayElement {
        private String name;

        public DumbArrayElement() {
        }

        private DumbArrayElement(String name) {
            this.name = name;
        }
    }

    @Entity
    private static final class DumbColl {
        @Id
        private ObjectId id;
        private LocalDateTime localDateTime;
        private String opaqueId;
        private List<DumbArrayElement> fromArray;

        private DumbColl() {
        }

        private DumbColl(String opaqueId) {
            this.opaqueId = opaqueId;
        }
    }

    @Entity
    private static class EmbeddedObjTest {
        private String name;
        private String value;

        public EmbeddedObjTest() {
        }

        public EmbeddedObjTest(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    @Entity
    public static class Log {
        private long receivedTs;
        private String value;

        public Log() {
        }

        public Log(long value) {
            this.value = "Log" + value;
            receivedTs = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Log log)) {
                return false;
            }
            return receivedTs == log.receivedTs && Objects.equals(value, log.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(receivedTs, value);
        }

        @Override
        public String toString() {
            return format("EntityLog{receivedTs=%s, value='%s'}", receivedTs, value);
        }
    }

    @Entity(useDiscriminator = false)
    private static class LogHolder {
        @Id
        private ObjectId id;
        @Indexed
        private String uuid;
        private Log log;
        private List<Log> logs = new ArrayList<>();
        private Document raw;

        public Log getLog() {
            return log;
        }

        public void setLog(Log log) {
            this.log = log;
        }

        public List<Log> getLogs() {
            return logs;
        }

        public void setLogs(List<Log> logs) {
            this.logs = logs;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
            result = 31 * result + (log != null ? log.hashCode() : 0);
            result = 31 * result + (logs != null ? logs.hashCode() : 0);
            result = 31 * result + (raw != null ? raw.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LogHolder)) {
                return false;
            }

            final LogHolder logHolder = (LogHolder) o;

            if (!Objects.equals(id, logHolder.id)) {
                return false;
            }
            if (!Objects.equals(uuid, logHolder.uuid)) {
                return false;
            }
            if (!Objects.equals(log, logHolder.log)) {
                return false;
            }
            if (!Objects.equals(logs, logHolder.logs)) {
                return false;
            }
            return Objects.equals(raw, logHolder.raw);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", LogHolder.class.getSimpleName() + "[", "]")
                    .add("id=" + id)
                    .add("uuid='" + uuid + "'")
                    .add("log=" + log)
                    .add("logs=" + logs)
                    .add("raw=" + raw)
                    .toString();
        }

        @PreLoad
        public void preload(Document raw) {
            this.raw = raw;
        }
    }

    @Entity
    public static class MapsOfStuff {
        @Id
        private ObjectId id;
        private final Map<String, TestEntity> map = new HashMap<>();

    }

    @Entity
    private static final class Parent {
        @Id
        private ObjectId id;
        private final Set<Child> children = new HashSet<>();
    }

    public static class Stuff1 extends TestEntity {
        private String foo;
    }

    public static class Stuff2 extends TestEntity {
        private String bar;
    }

    @Entity
    private static class TestMapWithEnumKey {
        @Id
        private ObjectId id;
        private Map<TestEnum, EmbeddedObjTest> map;

    }

    @DataProvider
    private static Iterator<TranslationParams> paths() {
        List<TranslationParams> list = new ArrayList<>();

        list.addAll(prepParams("stars", "s"));
        assertEquals(list.size(), countUpdateOperators(), "Should have checks for all UpdateOperators methods");
        list.addAll(prepParams("s", "s"));

        list.addAll(prepParams("address.street", "addr.address_street"));
        list.addAll(prepParams("address.address_street", "addr.address_street"));

        list.addAll(prepParams("addr.street", "addr.address_street"));
        list.addAll(prepParams("addr.address_street", "addr.address_street"));

        return list.iterator();
    }

    private static int countUpdateOperators() {
        // dec by 1 to exclude set(Entity) since there will be no path translation to check
        return UpdateOperators.class.getDeclaredMethods().length - 1;
    }

    @NotNull
    private static List<TranslationParams> prepParams(String updateName, String mappedName) {
        LocalTime now = LocalTime.of(15, 16, 17);
        Date date = new Date(107, 14, 15);
        return List.of(
                new TranslationParams(updateName, mappedName, addToSet(updateName, "MARKER")),
                new TranslationParams(updateName, mappedName, addToSet(updateName, List.of("MARKER"))),
                new TranslationParams(updateName, mappedName, and(updateName, 42)),
                new TranslationParams(updateName, mappedName, bit(updateName, 42)),
                new TranslationParams(updateName, mappedName, currentDate(updateName)),
                new TranslationParams(updateName, mappedName, dec(updateName)),
                new TranslationParams(updateName, mappedName, dec(updateName, 42)),
                new TranslationParams(updateName, mappedName, inc(updateName)),
                new TranslationParams(updateName, mappedName, inc(updateName, 42)),
                new TranslationParams(updateName, mappedName, max(updateName, 42)),
                new TranslationParams(updateName, mappedName, max(updateName, now)),
                new TranslationParams(updateName, mappedName, max(updateName, date)),
                new TranslationParams(updateName, mappedName, min(updateName, 42)),
                new TranslationParams(updateName, mappedName, min(updateName, now)),
                new TranslationParams(updateName, mappedName, min(updateName, date)),
                new TranslationParams(updateName, mappedName, mul(updateName, 42)),
                new TranslationParams(updateName, mappedName, or(updateName, 42)),
                new TranslationParams(updateName, mappedName, pop(updateName)),
                new TranslationParams(updateName, mappedName, pull(updateName, eq("name", "MARKER"))),
                new TranslationParams(updateName, mappedName, pull(updateName, "carrots")),
                new TranslationParams(updateName, mappedName, pullAll(updateName, List.of("MARKER"))),
                new TranslationParams(updateName, mappedName, push(updateName, "MARKER")),
                new TranslationParams(updateName, mappedName, push(updateName, List.of("MARKER"))),
                new TranslationParams(updateName, mappedName, rename(updateName, "MARKER")),
                new TranslationParams(updateName, mappedName, set(updateName, "MARKER")),
                new TranslationParams(updateName, mappedName, setOnInsert(Map.of(mappedName, "MARKER"))),
                new TranslationParams(updateName, mappedName, unset(updateName, updateName, updateName, updateName)),
                new TranslationParams(updateName, mappedName, xor(updateName, 42)));
    }

    public record TranslationParams(String updateName, String mappedName, UpdateOperator operator) {
    }

    @Test(dataProvider = "paths")
    public void testPathTranslations(TestUpdateOperations.TranslationParams params) {
        CodecRegistry registry = getDs().getCodecRegistry();
        Operations value = new Operations(getDs(), getMapper().getEntityModel(Hotel.class), List.of(params.operator), true);

        var json = toString(value.toDocument(getDs()));

        String format = format("\"%s\"", params.mappedName);
        boolean contains = json.contains(format);
        assertTrue(contains, format("failed to find '%s' in:%n%s", format, json));
    }

}
