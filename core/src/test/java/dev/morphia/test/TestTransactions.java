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
import dev.morphia.test.util.ActionTestOptions;
import dev.morphia.transactions.MorphiaSession;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.mongodb.ClientSessionOptions.builder;
import static com.mongodb.WriteConcern.MAJORITY;
import static dev.morphia.query.updates.UpdateOperators.inc;

/*@Tags(@Tag("transactions"))*/
public class TestTransactions extends dev.morphia.test.TemplatedTestBase {
    @BeforeEach
    public void before() {
        checkForReplicaSet();
        getDs().save(new Rectangle(1, 1));
        getDs().find(Rectangle.class).findAndDelete();
        getDs().save(new User("", LocalDate.now()));
        getDs().find(User.class).findAndDelete();
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

                    Assertions.assertNotNull(datastore.find(Rectangle.class).first());
                    Assertions.assertNotNull(session.find(Rectangle.class).first());

                    session.delete(rectangle);

                    Assertions.assertNotNull(datastore.find(Rectangle.class).first());
                    Assertions.assertNull(session.find(Rectangle.class).first());
                    return null;
                });

    }

    @Test
    public void insert() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction(session -> {
            session.insert(rectangle);

            Assertions.assertNull(getDs().find(Rectangle.class).first());
            Assertions.assertEquals(1, session.find(Rectangle.class).count());

            return null;
        });

        Assertions.assertNotNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void insertList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
                new Rectangle(1, 1));

        getDs().withTransaction(session -> {
            session.insert(rectangles);

            Assertions.assertNull(getDs().find(Rectangle.class).first());
            Assertions.assertEquals(rectangles, session.find(Rectangle.class).iterator().toList());

            return null;
        });

        Assertions.assertEquals(2, getDs().find(Rectangle.class).count());
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

                Assertions.assertNull(getDs().find(Rectangle.class).first());
                Assertions.assertNull(getDs().find(User.class).first());
                Assertions.assertNotNull(session.find(Rectangle.class).first());
                Assertions.assertNotNull(session.find(User.class).first());

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

        Assertions.assertNotNull(getDs().find(Rectangle.class).first());
        Assertions.assertNotNull(getDs().find(User.class).first());
    }

    @Test
    public void merge() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().save(rectangle);
        Assertions.assertEquals(1, getDs().find(Rectangle.class).count());

        getDs().withTransaction(session -> {

            Assertions.assertEquals(new Rectangle(1, 1), getDs().find(Rectangle.class).first());
            Assertions.assertEquals(new Rectangle(1, 1), session.find(Rectangle.class).first());

            rectangle.setWidth(20);
            session.merge(rectangle);

            Assertions.assertEquals(1, getDs().find(Rectangle.class).first().getWidth(), 0.5);
            Assertions.assertEquals(20, session.find(Rectangle.class).first().getWidth(), 0.5);

            return null;
        });

        Assertions.assertEquals(20, getDs().find(Rectangle.class).first().getWidth(), 0.5);
    }

    @Test
    @DisplayName("transactional aggregations")
    public void aggregation() {
        getDs().withTransaction(session -> {
            testPipeline(
                    new ActionTestOptions().orderMatters(false),
                    aggregation -> {
                        loadData("aggTest2", 2);
                        return aggregation
                                .pipeline(Lookup.lookup("aggTest2")
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

            Assertions.assertNull(getDs().find(Rectangle.class).first());

            Rectangle modified = session.find(Rectangle.class)
                    .modify(inc("width", 13));

            Assertions.assertNull(getDs().find(Rectangle.class).first());
            Assertions.assertEquals(modified.getWidth(), rectangle.getWidth(), 0.5);
            Assertions.assertEquals(session.find(Rectangle.class)
                    .first().getWidth(), rectangle.getWidth() + 13, 0.5);

            return null;
        });

        Assertions.assertEquals(rectangle.getWidth() + 13, getDs().find(Rectangle.class).first().getWidth(), 0.5);
    }

    @Test
    public void remove() {
        Rectangle rectangle = new Rectangle(1, 1);
        getDs().save(rectangle);

        getDs().withTransaction(session -> {

            Assertions.assertNotNull(getDs().find(Rectangle.class).first());
            Assertions.assertNotNull(session.find(Rectangle.class).first());

            session.find(Rectangle.class)
                    .delete();

            Assertions.assertNotNull(getDs().find(Rectangle.class).first());
            Assertions.assertNull(session.find(Rectangle.class).first());
            return null;
        });

        Assertions.assertNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void save() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction(session -> {
            session.save(rectangle);

            Assertions.assertNull(getDs().find(Rectangle.class).first());
            Assertions.assertNotNull(session.find(Rectangle.class).first());

            rectangle.setWidth(42);
            session.save(rectangle);

            Assertions.assertNull(getDs().find(Rectangle.class).first());
            Assertions.assertEquals(42, session.find(Rectangle.class).first().getWidth(), 0.5);

            return null;
        });

        Assertions.assertNotNull(getDs().find(Rectangle.class).first());
    }

    @Test
    public void saveList() {
        List<Rectangle> rectangles = List.of(new Rectangle(5, 7),
                new Rectangle(1, 1));

        getDs().withTransaction(session -> {
            session.save(rectangles);

            Assertions.assertNull(getDs().find(Rectangle.class).first());
            Assertions.assertEquals(2, session.find(Rectangle.class).count());

            return null;
        });

        Assertions.assertEquals(2, getDs().find(Rectangle.class).count());
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

            Assertions.assertEquals(1, list.size());

            return null;
        });
    }

    @Test
    public void update() {
        Rectangle rectangle = new Rectangle(1, 1);

        getDs().withTransaction(session -> {
            session.save(rectangle);

            Assertions.assertNull(getDs().find(Rectangle.class).first());

            session.find(Rectangle.class)
                    .update(inc("width", 13));

            Assertions.assertEquals(rectangle.getWidth() + 13, session.find(Rectangle.class).first().getWidth(), 0.5);

            Assertions.assertNull(getDs().find(Rectangle.class).first());
            return null;
        });

        Assertions.assertEquals(rectangle.getWidth() + 13, getDs().find(Rectangle.class).first().getWidth(), 0.5);
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
        Assertions.assertEquals(1, deleteResult.getDeletedCount());

        Assertions.assertEquals(refId, root.getR().getId());
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
