package dev.morphia.mapping;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

import com.mongodb.WriteConcern;
import com.mongodb.lang.Nullable;

import dev.morphia.EntityListener;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.ExternalEntity;
import dev.morphia.annotations.PostLoad;
import dev.morphia.annotations.PostPersist;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.PrePersist;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;

import org.bson.Document;

/**
 * @morphia.internal
 * @hidden
 */
@MorphiaInternal
public interface Mapper {

    /**
     * Special name that can never be used. Used as default for some fields to indicate default state.
     *
     * @morphia.internal
     */
    @MorphiaInternal
    String IGNORED_FIELDNAME = ".";

    @MorphiaInternal
    List<Class<? extends Annotation>> MAPPING_ANNOTATIONS = List.of(Entity.class, ExternalEntity.class);

    @MorphiaInternal
    List<Class<? extends Annotation>> LIFECYCLE_ANNOTATIONS = List.of(PrePersist.class,
            PreLoad.class,
            PostPersist.class,
            PostLoad.class);

    /**
     * Adds an {@link EntityListener}
     *
     * @param ei the interceptor to add
     * @deprecated use {@link dev.morphia.annotations.EntityListeners} to define any lifecycle event listeners
     */
    @Deprecated(forRemoval = true, since = "2.4.0")
    void addInterceptor(EntityListener<?> ei);

    /**
     * @return the copied Mapper
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    Mapper copy();

    /**
     * @param type the class
     * @return the id property model
     * @hidden
     * @morphia.internal
     * @since 2.2
     */
    @MorphiaInternal
    PropertyModel findIdProperty(Class<?> type);

    /**
     * Gets the class as defined by any discriminator field
     *
     * @param document the document to check
     * @param <T>      the class type
     * @return the class reference. might be null
     */
    @Nullable
    <T> Class<T> getClass(Document document);

    /**
     * @param discriminator the lookup value
     * @return the class mapped to this discriminator value
     */
    Class getClass(String discriminator);

    /**
     * Looks up the class mapped to a named collection.
     *
     * @param collection the collection name
     * @param <T>        the class type
     * @return the Class mapped to this collection name
     * @morphia.internal
     */
    @MorphiaInternal
    <T> Class<T> getClassFromCollection(String collection);

    /**
     * Finds all the types mapped to a named collection
     *
     * @param collection the collection to check
     * @return the mapped types
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    List<EntityModel> getClassesMappedToCollection(String collection);

    /**
     * @return the DiscriminatorLookup in use
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    DiscriminatorLookup getDiscriminatorLookup();

    /**
     * Tries to get the {@link EntityModel} for the object (type). If it isn't mapped, but can be mapped, create a new
     * class and cache it (without validating). If it isn't mapped and cannot be mapped, return empty.
     *
     * @param type the type to process
     * @return optional EntityModel for the object given or empty
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    Optional<EntityModel> tryGetEntityModel(Class type);

    /**
     * Gets the {@link EntityModel} for the object (type). If it isn't mapped, create a new class and cache it (without validating).
     *
     * @param type the type to process
     * @return the EntityModel for the object given
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    EntityModel getEntityModel(Class type);

    /**
     * @param type the class to map
     * @return the EntityModel or null
     */
    @Nullable
    EntityModel mapEntity(@Nullable Class type);

    /**
     * Gets the ID value for an entity
     *
     * @param entity the entity to process
     * @return the ID value
     */
    @Nullable
    Object getId(@Nullable Object entity);

    /**
     * Gets list of {@link EntityListener}s
     *
     * @return the Interceptors
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    List<EntityListener<?>> getListeners();

    /**
     * @return collection of EntityModels
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    List<EntityModel> getMappedEntities();

    /**
     * @return the options used by this Mapper
     */
    MorphiaConfig getConfig();

    /**
     * Gets the write concern for entity or returns the default write concern for this datastore
     *
     * @param clazz the class to use when looking up the WriteConcern
     * @return the write concern for the type
     * @hidden
     * @morphia.internal
     */
    @Nullable
    WriteConcern getWriteConcern(Class clazz);

    /**
     * @return true if there are global interceptors defined
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    boolean hasListeners();

    /**
     * Checks if a type is mappable or not
     *
     * @param type the class to check
     * @param <T>  the type
     * @return true if the type is mappable
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    <T> boolean isMappable(@Nullable Class<T> type);

    /**
     * Checks to see if a Class has been mapped.
     *
     * @param c the Class to check
     * @return true if the Class has been mapped
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    boolean isMapped(Class c);

    /**
     * Maps a set of classes
     *
     * @param entityClasses the classes to map
     * @return the EntityModel references
     * @hidden
     */
    List<EntityModel> map(Class... entityClasses);

    /**
     * Maps a set of classes
     *
     * @hidden
     * @param classes the classes to map
     * @return the list of mapped classes
     */
    List<EntityModel> map(List<Class<?>> classes);

    /**
     * Tries to map all classes in the package specified.
     *
     * @param packageName the name of the package to process
     * @hidden
     * @since 2.4
     */
    void map(String packageName);

    /**
     * Tries to map all classes in the package specified.
     *
     * @hidden
     * @param packageName the name of the package to process
     */
    void mapPackage(String packageName);

    /**
     * Updates a query with any discriminators from subtypes if polymorphic queries are enabled
     *
     * @param model the query model
     * @param query the query document
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    void updateQueryWithDiscriminators(EntityModel model, Document query);

    /**
     * @param entityModel the model to register
     * @return the model
     * @hidden
     * @morphia.internal
     * @since 2.3
     */
    @MorphiaInternal
    EntityModel register(EntityModel entityModel);
}
