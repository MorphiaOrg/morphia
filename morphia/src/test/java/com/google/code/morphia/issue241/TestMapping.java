package com.google.code.morphia.issue241;


import java.net.UnknownHostException;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.google.code.morphia.DatastoreImpl;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.dao.BasicDAO;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;


/**
 * Unit test for testing morphia mappings with generics.
 */
public class TestMapping {

  final Morphia morphia = new Morphia();

  Mongo         mongo;
  DatastoreImpl datastore;
  final MongoClientURI uri = new MongoClientURI("mongodb://127.0.0.1:27017");


  @Before
  public void setUp() {
    try {
      mongo = new MongoClient(uri);
      datastore = new DatastoreImpl(morphia, mongo, "MY_DB");
    } catch (UnknownHostException unknownHostException) {
    } catch (MongoException mongoException) {
    }
  }

  @After
  public void tearDown() {
  }

  @SuppressWarnings("rawtypes") @Test
  public void testMapping() {
    final BasicDAO<Message, ObjectId> messageDAO = new BasicDAO<Message, ObjectId>(Message.class, datastore);
    Assert.assertNotNull(messageDAO);
  }

  @Entity
  private static class Message<U extends User> {

    @Id
    private ObjectId id;
    private U        user;

    public U getUser() {
      return user;
    }

    public void setUser(final U user) {
      this.user = user;
    }
  }

  @Entity
  private static class User {
    @Id
    private ObjectId id;

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final User other = (User) obj;
      return !(id != other.id && (id == null || !id.equals(other.id)));
    }

    @Override
    public int hashCode() {
      int hash = 3;
      hash = 97 * hash + (id != null ? id.hashCode() : 0);
      return hash;
    }
  }
}
