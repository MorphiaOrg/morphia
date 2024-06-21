package dev.morphia.test;

import java.time.LocalDate;
import java.util.List;

import com.mongodb.MongoQueryException;
import com.mongodb.TransactionOptions;
import com.mongodb.client.result.DeleteResult;

import dev.morphia.Datastore;
import dev.morphia.aggregation.stages.Lookup;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Reference;
import dev.morphia.query.filters.Filters;
import dev.morphia.test.mapping.lazy.TestLazyCircularReference.ReferencedEntity;
import dev.morphia.test.mapping.lazy.TestLazyCircularReference.RootEntity;
import dev.morphia.test.models.Rectangle;
import dev.morphia.test.models.User;
import dev.morphia.transactions.MorphiaSession;

import org.bson.types.ObjectId;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.mongodb.ClientSessionOptions.builder;
import static com.mongodb.WriteConcern.MAJORITY;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

//@Tags(@Tag("transactions"))
public class TestTransactions extends dev.morphia.test.TemplatedTestBase {
    @BeforeMethod
    public void before() {
        checkForReplicaSet();
        getDs().save(new Rectangle(1, 1));
        getDs().find(Rectangle.class).findAndDelete();
        getDs().save(new User("", LocalDate.now()));
        getDs().find(User.class).findAndDelete();
    }

    @AfterClass
    @Override
    public void testCoverage() {
    }

    @Test
    public void delete() {
        Rectangle rectangle = new Rectangle(1, 1);
        Datastore datastore = getDs();
        datastore.save(rectangle);

        datastore.withTransaction(builder()
                .defaultTransactionOptions(TransactionOptions.builder()
                        .writeConcern(MAJORITY)
                        .build())
                .build(), session -> {

                    assertNotNull(datastore.find(Rectangle.class).first());
                    assertNotNull(session.find(Rectangle.class).first());

                    session.delete(rectangle);

                    assertNotNull(datastore.find(Rectangle.class).first());
                    assertNull(session.find(Rectangle.class).first());
                    return null;
                });

    }

    @Test
    public void insert() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction(session -> {
            session.insert(rectangle);

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(session.find(Rectangle.class).count(), 1);

            return null;
        });

        assertNotNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void insertList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
                new Rectangle(1, 1));

        getDs().withTransaction(session -> {
            session.insert(rectangles);

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(session.find(Rectangle.class).iterator().toList(), rectangles);

            return null;
        });

        assertEquals(getDs().find(Rectangle.class).count(), 2);
    }

    @Test
    public void manual() {
        boolean success = false;
        int count = 0;
        while (!success && count < 5) {
            try (MorphiaSession session = getDs().startSession()) {
                session.startTransaction();

                Rectangle rectangle = new Rectangle(1, 1);
                session.save(rectangle);

                session.save(new User("transactions", LocalDate.now()));

                assertNull(getDs().find(Rectangle.class).first());
                assertNull(getDs().find(User.class).first());
                assertNotNull(session.find(Rectangle.class).first());
                assertNotNull(session.find(User.class).first());

                session.commitTransaction();
                success = true;
            } catch (MongoQueryException e) {
                if (e.getErrorCode() == 251 && e.getErrorMessage().contains("has been aborted")) {
                    count++;
                } else {
                    throw e;
                }
            } catch (RuntimeException e) {
                throw e;
            }
        }

        assertNotNull(getDs().find(Rectangle.class).first());
        assertNotNull(getDs().find(User.class).first());
    }

    @Test
    public void merge() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().save(rectangle);
        assertEquals(getDs().find(Rectangle.class).count(), 1);

        getDs().withTransaction(session -> {

            assertEquals(getDs().find(Rectangle.class).first(), new Rectangle(1, 1));
            assertEquals(session.find(Rectangle.class).first(), new Rectangle(1, 1));

            rectangle.setWidth(20);
            session.merge(rectangle);

            assertEquals(getDs().find(Rectangle.class).first().getWidth(), 1, 0.5);
            assertEquals(session.find(Rectangle.class).first().getWidth(), 20, 0.5);

            return null;
        });

        assertEquals(getDs().find(Rectangle.class).first().getWidth(), 20, 0.5);
    }

    @Test(testName = "transactional aggregations")
    public void aggregation() {
        getDs().withTransaction(session -> {
            testPipeline(
                    new dev.morphia.test.util.ActionTestOptions().serverVersion(ServerVersion.ANY).removeIds(false).orderMatters(false),
                    aggregation -> {
                        loadData("aggTest2", 2);
                        return aggregation
                                .lookup(Lookup.lookup("aggTest2")
                                        .localField("item")
                                        .foreignField("sku")
                                        .as("inventory_docs"));
                    });
            return null;
        });
    }

    @Test
    public void modify() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction(session -> {
            session.save(rectangle);

            assertNull(getDs().find(Rectangle.class).first());

            Rectangle modified = session.find(Rectangle.class)
                    .modify(inc("width", 13));

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(rectangle.getWidth(), modified.getWidth(), 0.5);
            assertEquals(rectangle.getWidth() + 13, session.find(Rectangle.class)
                    .first().getWidth(), 0.5);

            return null;
        });

        assertEquals(getDs().find(Rectangle.class).first().getWidth(), rectangle.getWidth() + 13, 0.5);
    }

    @Test
    public void remove() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDs().save(rectangle);

        getDs().withTransaction(session -> {

            assertNotNull(getDs().find(Rectangle.class).first());
            assertNotNull(session.find(Rectangle.class).first());

            session.find(Rectangle.class)
                    .delete();

            assertNotNull(getDs().find(Rectangle.class).first());
            assertNull(session.find(Rectangle.class).first());
            return null;
        });

        assertNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void save() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction(session -> {
            session.save(rectangle);

            assertNull(getDs().find(Rectangle.class).first());
            assertNotNull(session.find(Rectangle.class).first());

            rectangle.setWidth(42);
            session.save(rectangle);

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(session.find(Rectangle.class).first().getWidth(), 42, 0.5);

            return null;
        });

        assertNotNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void saveList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
                new Rectangle(1, 1));

        getDs().withTransaction(session -> {
            session.save(rectangles);

            assertNull(getDs().find(Rectangle.class).first());
            assertEquals(session.find(Rectangle.class).count(), 2);

            return null;
        });

        assertEquals(getDs().find(Rectangle.class).count(), 2);
    }

    @Test
    public void testTransactions() {
        getDs().withTransaction(session -> {
            // save company
            Company company = new Company();
            company.name = "test";
            company = session.save(company);

            // save employee with reference to company
            Employee employee = new Employee();
            employee.email = "test@test.com";
            employee.company = company;
            session.save(employee);

            var list = session.find(Employee.class).iterator().toList();

            assertEquals(list.size(), 1);

            return null;
        });
    }

    @Test
    public void update() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction(session -> {
            session.save(rectangle);

            assertNull(getDs().find(Rectangle.class).first());

            session.find(Rectangle.class)
                    .update(inc("width", 13));

            assertEquals(session.find(Rectangle.class).first().getWidth(), rectangle.getWidth() + 13, 0.5);

            assertNull(getDs().find(Rectangle.class).first());
            return null;
        });

        assertEquals(getDs().find(Rectangle.class).first().getWidth(), rectangle.getWidth() + 13, 0.5);
    }

    @Test
    public void testFetchAfterTransactionalDelete() {
        checkForProxyTypes();
        checkForReplicaSet();

        RootEntity root = new RootEntity();
        final ReferencedEntity ref = new ReferencedEntity();
        ref.setFoo("bar");

        root.setR(ref);

        final ObjectId refId = getDs().save(ref).getId();
        final ObjectId rootId = getDs().save(root).getId();

        root = getDs().find(RootEntity.class).filter(Filters.eq("_id", rootId)).first();

        final DeleteResult deleteResult = getDs().withTransaction((session) -> session.find(RootEntity.class).delete());
        Assert.assertEquals(deleteResult.getDeletedCount(), 1);

        Assert.assertEquals(root.getR().getId(), refId);
    }

    @Entity
    private static class Company {
        @Id
        private ObjectId id;
        String name;
    }

    @Entity
    private static class Employee {
        @Id
        private ObjectId id;
        String email;
        @Reference
        Company company;
    }
}
