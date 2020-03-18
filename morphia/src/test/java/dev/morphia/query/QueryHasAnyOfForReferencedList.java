package dev.morphia.query;

import dev.morphia.TestBase;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Reference;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static org.junit.Assert.assertEquals;

@Category(Reference.class)
public class QueryHasAnyOfForReferencedList extends TestBase {

    @Test
    public void testInQuery() {

        Plan plan1 = getDs().save(new Plan("Trial 1"));
        Plan plan2 = getDs().save(new Plan("Trial 2"));

        Org org1 = getDs().save(new Org("Test Org1", plan1));
        Org org2 = getDs().save(new Org("Test Org2", plan2));

        long count = getDs().find(Org.class)
                            .filter(eq("name", "Test Org1"))
                            .count();
        assertEquals(1, count);

        List<Plan> plans = new ArrayList<>();
        plans.add(plan1);

        count = getDs().find(Org.class)
                       .field("plan").hasAnyOf(plans)
                       .count();
        assertEquals(1, count);

        plans = new ArrayList<>();
        plans.add(plan1);
        plans.add(plan2);

        count = getDs().find(Org.class).field("plan").hasAnyOf(plans).count();
        assertEquals(2, count);
    }

    @Entity(useDiscriminator = false)
    private static class Plan implements Serializable {

        @Id
        private ObjectId id;
        @Property("name")
        private String name;

        public Plan() {
        }

        public Plan(final String name) {
            this.name = name;
        }
    }

    @Entity(useDiscriminator = false)
    private static class Org implements Serializable {
        @Id
        private ObjectId id;
        @Property("name")
        private String name;
        @Reference("plan")
        private Plan plan;

        public Org(final String name, final Plan plan) {
            this.name = name;
            this.plan = plan;
        }

        public Org() {
        }
    }

}
