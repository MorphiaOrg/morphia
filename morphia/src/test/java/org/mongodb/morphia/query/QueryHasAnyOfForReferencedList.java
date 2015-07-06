package org.mongodb.morphia.query;

import org.bson.types.ObjectId;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class QueryHasAnyOfForReferencedList extends TestBase {

    @Test
    public void testInQuery() throws Exception {

        Plan plan1 = new Plan();
        plan1.name = "Trial";

        Plan plan2 = new Plan();
        plan2.name = "Trial";

        getDs().save(plan1);
        getDs().save(plan2);

        Org org1 = new Org();
        org1.plan = plan1;
        org1.name = "Test Org1";

        Org org2 = new Org();
        org2.plan = plan2;
        org2.name = "Test Org2";

        getDs().save(org1);
        getDs().save(org2);

        long count = getDs().createQuery(Org.class).field("name").equal("Test Org1").countAll();
        assertEquals(1, count);

        List<Plan> plans = new ArrayList<Plan>();
        plans.add(plan1);

        count = getDs().createQuery(Org.class).field("plan").hasAnyOf(plans).countAll();
        assertEquals(1, count);

        plans = new ArrayList<Plan>();
        plans.add(plan1);
        plans.add(plan2);

        count = getDs().createQuery(Org.class).field("plan").hasAnyOf(plans).countAll();
        assertEquals(2, count);
    }

    @Entity(noClassnameStored = true)
    private static class Plan implements Serializable {

        @Id
        private ObjectId id;
        @Property("name")
        private String name;
    }

    @Entity(noClassnameStored = true)
    private static class Org implements Serializable {
        @Id
        private ObjectId id;
        @Property("name")
        private String name;
        @Reference("plan")
        private Plan plan;
    }

}
