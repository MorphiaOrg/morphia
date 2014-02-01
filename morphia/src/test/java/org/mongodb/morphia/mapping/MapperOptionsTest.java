package org.mongodb.morphia.mapping;


import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Id;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


/**
 * @author scott hernandez
 */
public class MapperOptionsTest extends TestBase {

    private static class HasList implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        private List<String> names = new ArrayList<String>();

        HasList() {
        }
    }

    private static class HasMap implements Serializable {
        @Id
        private ObjectId id = new ObjectId();
        private Map<String, String> properties = new HashMap<String, String>();

        HasMap() {
        }
    }

    @Test
    public void emptyListStoredWithOptions() throws Exception {
        final HasList hl = new HasList();

        //Test default behavior
        getDs().save(hl);
        DBObject dbObj = getDs().getCollection(HasList.class).findOne();
        Assert.assertFalse("field exists, value =" + dbObj.get("names"), dbObj.containsField("names"));

        //Test default storing empty list/array with storeEmpties option
        getMorphia().getMapper().getOptions().setStoreEmpties(true);
        getDs().save(hl);
        dbObj = getDs().getCollection(HasList.class).findOne();
        Assert.assertTrue("field missing", dbObj.containsField("names"));

        //Test opposite from above
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        getDs().save(hl);
        dbObj = getDs().getCollection(HasList.class).findOne();
        Assert.assertFalse("field exists, value =" + dbObj.get("names"), dbObj.containsField("names"));
    }

    @Test
    public void emptyMapStoredWithOptions() throws Exception {
        final HasMap hm = new HasMap();

        //Test default behavior
        getDs().save(hm);
        DBObject dbObj = getDs().getCollection(HasMap.class).findOne();
        Assert.assertFalse("field exists, value =" + dbObj.get("properties"), dbObj.containsField("properties"));

        //Test default storing empty map with storeEmpties option
        getMorphia().getMapper().getOptions().setStoreEmpties(true);
        getDs().save(hm);
        dbObj = getDs().getCollection(HasMap.class).findOne();
        Assert.assertTrue("field missing", dbObj.containsField("properties"));

        //Test opposite from above
        getMorphia().getMapper().getOptions().setStoreEmpties(false);
        getDs().save(hm);
        dbObj = getDs().getCollection(HasMap.class).findOne();
        Assert.assertFalse("field exists, value =" + dbObj.get("properties"), dbObj.containsField("properties"));
    }
}
