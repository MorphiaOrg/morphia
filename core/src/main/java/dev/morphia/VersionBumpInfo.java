package dev.morphia;

import com.mongodb.lang.Nullable;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.query.Query;
import org.bson.Document;

import static dev.morphia.query.filters.Filters.eq;

@MorphiaInternal
public class VersionBumpInfo {
    private final Long oldVersion;
    private final boolean versioned;
    private final Long newVersion;
    private final PropertyModel idProperty;
    private final PropertyModel versionProperty;
    private final Object entity;

    VersionBumpInfo() {
        versioned = false;
        newVersion = null;
        oldVersion = null;
        idProperty = null;
        versionProperty = null;
        entity = null;
    }

    <T> VersionBumpInfo(T entity, PropertyModel idProperty, PropertyModel versionProperty, @Nullable Long oldVersion, Long newVersion) {
        this.entity = entity;
        this.idProperty = idProperty;
        versioned = true;
        this.newVersion = newVersion;
        this.oldVersion = oldVersion;
        this.versionProperty = versionProperty;
    }

    public Object entity() {
        return entity;
    }

    public void filter(Document filter) {
        if (versioned()) {
            filter.put(versionProperty.getMappedName(), oldVersion());
        }
    }

    public <T> void filter(Query<T> query) {
        if (versioned() && newVersion() != -1) {
            query.filter(eq(versionProperty.getMappedName(), oldVersion()));
        }

    }

    public Long newVersion() {
        return newVersion;
    }

    public Long oldVersion() {
        return oldVersion;
    }

    public void rollbackVersion() {
        if (entity != null && versionProperty != null) {
            versionProperty.setValue(entity, oldVersion);
        }
    }

    public boolean versioned() {
        return versioned;
    }
}
