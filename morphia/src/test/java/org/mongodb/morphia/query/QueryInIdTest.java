package org.mongodb.morphia.query;


import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.util.ArrayList;
import java.util.List;


/**
 * @author scotthernandez
 */
public class QueryInIdTest extends TestBase {

    @Entity("docs")
    private static class Doc {
        @Id
        private long id = 4;
    }

    @Test
    public void testInIdList() throws Exception {
        final Doc doc = new Doc();
        doc.id = 1;
        getDs().save(doc);

        // this works
        getDs().find(Doc.class).field("_id").equal(1).asList();

        final List<Long> idList = new ArrayList<Long>();
        idList.add(1L);
        // this causes an NPE
        getDs().find(Doc.class).field("_id").in(idList).asList();

    }

}
