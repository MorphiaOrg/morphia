package org.mongodb.morphia.mapping.classinfo;

import com.mongodb.DBObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ClassInfoPersister} that assumes that all class info is stored within one field and delegates to
 * a {@link ClassIdMapper} to get and parse the value from that one field
 */
public class DefaultClassInfoPersister implements ClassInfoPersister {

    /**
     * Special field used by morphia to support various possibly loading issues; will be replaced when discriminators
     * are implemented to support polymorphism
     */
    public static final String DEFAULT_DISCRIMINATOR_FIELD_NAME = "className";

    private final String discriminatorFieldName;
    private final ClassIdMapper classIdMapper;

    /**
     * Default constructor that provides backwards compatibility and uses {@link ClassNameClassIdMapper} and the
     * standard {@link DefaultClassInfoPersister#DEFAULT_DISCRIMINATOR_FIELD_NAME}
     * @see ClassNameClassIdMapper
     */
    public DefaultClassInfoPersister() {
        this(DEFAULT_DISCRIMINATOR_FIELD_NAME, new ClassNameClassIdMapper());
    }

    /**
     * Constructor that provides the ability to customize behavior with a different discriminator field and different
     * id mapping logic.
     *
     * @param discriminatorFieldName the field in which to store the class id
     * @param classIdMapper the strategy for producing a unique class id
     * @see ClassIdMapper
     */
    public DefaultClassInfoPersister(final String discriminatorFieldName, final ClassIdMapper classIdMapper) {
        this.discriminatorFieldName = discriminatorFieldName;
        this.classIdMapper = classIdMapper;
    }

    @Override
    public void addClassInfo(final Object entity, final DBObject dbObject, final Class<?> hint) {
        dbObject.put(discriminatorFieldName, classIdMapper.getId(entity));
    }

    @Override
    public <T> Class<T> getClass(final DBObject dbObject, final Class<?> hint) {
        return getClass(dbObject); // ignore hint
    }

    @Override
    public <T> Class<T> getClass(final DBObject dbObject) {
        final String classId = (String) dbObject.get(discriminatorFieldName);
        if (classId == null) {
            return null;
        }
        return classIdMapper.getClass(classId);
    }

    @Override
    public void setCaching(final boolean caching) {
        classIdMapper.setCaching(caching);
    }

    @Override
    public boolean isCaching() {
        return classIdMapper.isCaching();
    }

    /**
     * @return the cache of classnames
     */
    @Deprecated
    public Map<String, Class> getClassCache() {
        if (classIdMapper instanceof ClassNameClassIdMapper) {
            return ((ClassNameClassIdMapper) classIdMapper).getClassNameCache();
        }
        return new HashMap<String, Class>();
    }


    @Override
    public void removeClassInfo(final DBObject dbObject) {
        dbObject.removeField(DEFAULT_DISCRIMINATOR_FIELD_NAME);
    }

    @Override
    public void removeClassInfo(final DBObject dbObject, final Class<?> hint) {
        removeClassInfo(dbObject); // ignore hint
    }

    @Override
    public void addClassInfoToProjection(final DBObject projection, final Class<?> hint) {
        projection.put(DEFAULT_DISCRIMINATOR_FIELD_NAME, 1);
    }
}
