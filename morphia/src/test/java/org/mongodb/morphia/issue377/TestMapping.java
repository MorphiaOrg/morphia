package org.mongodb.morphia.issue377;

import java.awt.Color;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.mongodb.morphia.TestBase;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Serialized;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.cache.DefaultEntityCache;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;


/**
 * Unit test for testing morphia mappings with Serialized
 */
public class TestMapping extends TestBase {

    @Test
    public void testMapping() {
        final BasicDAO<User, ObjectId> messageDAO = new BasicDAO<User, ObjectId>(User.class, getDs());
        Assert.assertNotNull(messageDAO);
        
        Mapper mapper = new Mapper();

        User user = new User();
        user.id = 1;

        user.userObject = "just a String";
        DBObject dbObject = mapper.toDBObject(user);
        Object object = mapper.fromDBObject(User.class, dbObject, new DefaultEntityCache());
        Assert.assertEquals(user.userObject, ((User) object).userObject);
        
        user.userObject = 33;
        dbObject = mapper.toDBObject(user);
        object = mapper.fromDBObject(User.class, dbObject, new DefaultEntityCache());
        Assert.assertEquals(user.userObject, ((User) object).userObject);

        user.userObject = 33.3;
        dbObject = mapper.toDBObject(user);
        object = mapper.fromDBObject(User.class, dbObject, new DefaultEntityCache());
        Assert.assertEquals(user.userObject, ((User) object).userObject);
        
        user.userObject = Color.red;
        dbObject = mapper.toDBObject(user);
        object = mapper.fromDBObject(User.class, dbObject, new DefaultEntityCache());
        Assert.assertEquals(user.userObject, ((User) object).userObject);
        
        // Following test simulates the behaviour when only an entity is updated only in parts
        // (E.g called by UpdateOpsImpl.add()):
        user.userObject = Color.red;
        MappedClass mc = new MappedClass(User.class, mapper);
        MappedField mf = mc.getMappedField("userObject");
        // toMongoObject() does similar mapping to toDBObject() but may differ in details:
        Object dbValue = mapper.toMongoObject(mf, null/*MappedClass*/, user.userObject);
        dbObject = new BasicDBObject("userObject", dbValue);
        object = mapper.fromDBObject(User.class, dbObject, new DefaultEntityCache());
        Assert.assertEquals(user.userObject, ((User) object).userObject);
    }


    @Entity
    private static class User {
        @Id
        private Integer id;
        
        @Serialized
        private Object userObject;
    }
}