/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.mongodb.morphia;


import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Set;

import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MappingException;
import org.mongodb.morphia.mapping.cache.EntityCache;
import org.mongodb.morphia.utils.ReflectionUtils;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;


/**
 * @author Olafur Gauti Gudmundsson
 * @author Scott Hernandez
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Morphia {
  private final Mapper mapper;
  private MongoClient mongoClient;

  public Morphia() {
    this(Collections.<Class>emptySet());
  }
  
  /**
   * Constrauctor to configure your own mapper
   * @param myMapper
   */
  public Morphia( final Mapper myMapper) {
    this(Collections.<Class>emptySet());
    mapper = myMapper;
  }

  public Morphia(final Set<Class> classesToMap) {
    mapper = new Mapper();
    for (final Class c : classesToMap) {
      map(c);
    }
  }

  public synchronized Morphia map(final Class... entityClasses) {
    if (entityClasses != null && entityClasses.length > 0) {
      for (final Class entityClass : entityClasses) {
        if (!mapper.isMapped(entityClass)) {
          mapper.addMappedClass(entityClass);
        }
      }
    }
    return this;
  }

  public synchronized Morphia mapPackageFromClass(final Class clazz) {
    return mapPackage(clazz.getPackage().getName(), false);
  }

  /**
   * Tries to map all classes in the package specified. Fails if one of the classes is not valid for mapping.
   *
   * @param packageName the name of the package to process
   * @return the Morphia instance
   */
  public synchronized Morphia mapPackage(final String packageName) {
    return mapPackage(packageName, false);
  }

  /**
   * Tries to map all classes in the package specified.
   *
   * @param packageName the name of the package to process
   * @param ignoreInvalidClasses specifies whether to ignore classes in the package that cannot be mapped
   * @return the Morphia instance
   */
  public synchronized Morphia mapPackage(final String packageName, final boolean ignoreInvalidClasses) {
    try {
      for (final Class c : ReflectionUtils.getClasses(packageName)) {
        try {
          final Embedded embeddedAnn = ReflectionUtils.getClassEmbeddedAnnotation(c);
          final Entity entityAnn = ReflectionUtils.getClassEntityAnnotation(c);
          if (entityAnn != null || embeddedAnn != null) {
            map(c);
          }
        } catch (MappingException ex) {
          if (!ignoreInvalidClasses) {
            throw ex;
          }
        }
      }
      return this;
    } catch (IOException e) {
      throw new MappingException("Could not get map classes from package " + packageName, e);
    } catch (ClassNotFoundException e) {
      throw new MappingException("Could not get map classes from package " + packageName, e);
    }
  }

  /**
   * Check whether a specific class is mapped by this instance.
   *
   * @param entityClass the class we want to check
   * @return true if the class is mapped, else false
   */
  public boolean isMapped(final Class entityClass) {
    return mapper.isMapped(entityClass);
  }

  public <T> T fromDBObject(final Class<T> entityClass, final DBObject dbObject) {
    return fromDBObject(entityClass, dbObject, mapper.createEntityCache());
  }

  public <T> T fromDBObject(final Class<T> entityClass, final DBObject dbObject, final EntityCache cache) {
    if (!entityClass.isInterface() && !mapper.isMapped(entityClass)) {
      throw new MappingException("Trying to map to an unmapped class: " + entityClass.getName());
    }
    try {
      return (T) mapper.fromDBObject(entityClass, dbObject, cache);
    } catch (Exception e) {
      throw new MappingException("Could not map entity from DBObject", e);
    }
  }

  public DBObject toDBObject(final Object entity) {
    try {
      return mapper.toDBObject(entity);
    } catch (Exception e) {
      throw new MappingException("Could not map entity to DBObject", e);
    }
  }

  public Mapper getMapper() {
    return mapper;
  }

  /**
   * This will create a new Mongo instance; it is best to use a Mongo singleton instance
   * @deprecated use #createDatastore(com.mongodb.Mongo,java.lang.String) which a MongoClient instance which will authenticate for you
   */
  public Datastore createDatastore(final String dbName) {
    return createDatastore(getDefaultMongoClient(), dbName, null, null);
  }

  /**
   * This will create a new Mongo instance; it is best to use a Mongo singleton instance
   * @deprecated use #createDatastore(com.mongodb.Mongo,java.lang.String) which a MongoClient instance which will authenticate for you
   */
  public Datastore createDatastore(final String dbName, final String user, final char[] pw) {
    try {
      return createDatastore(getDefaultMongoClient(), dbName, user, pw);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private MongoClient getDefaultMongoClient() {
    if (mongoClient == null) {
      final MongoClientOptions.Builder builder = MongoClientOptions.builder();
      builder.connectionsPerHost(100);
      builder.threadsAllowedToBlockForConnectionMultiplier(5);
      builder.maxWaitTime(1000 * 60 * 2);
      builder.connectTimeout(1000 * 10);
      builder.socketTimeout(0);
      builder.socketKeepAlive(false);
      builder.autoConnectRetry(false);
      builder.maxAutoConnectRetryTime(0);
      builder.readPreference(ReadPreference.primary());
      builder.writeConcern(WriteConcern.ACKNOWLEDGED);

      try {
        mongoClient = new MongoClient("localhost", builder.build());
      } catch (UnknownHostException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    return mongoClient;
  }

  /**
   * It is best to use a Mongo singleton instance here.
   * @deprecated use #createDatastore(com.mongodb.Mongo,java.lang.String) which a MongoClient instance which will authenticate for you
   */
  public Datastore createDatastore(final Mongo mon, final String dbName, final String user, final char[] pw) {
    return new DatastoreImpl(this, mon, dbName, user, pw);
  }

  /**
   * It is best to use a Mongo singleton instance here.
   */
  public Datastore createDatastore(final Mongo mongo, final String dbName) {
    return createDatastore(mongo, dbName, null, null);
  }

}
