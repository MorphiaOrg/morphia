package dev.morphia;

import com.mongodb.lang.Nullable;
import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.codec.pojo.PropertyModel;

@MorphiaInternal
public class VersionBumpInfo {
    private final Long oldVersion;
    private final boolean versioned;
    private final Long newVersion;
    private final PropertyModel versionProperty;
    private final Object entity;

    VersionBumpInfo() {
        versioned = false;
        newVersion = null;
        oldVersion = null;
        versionProperty = null;
        entity = null;
    }

    <T> VersionBumpInfo(T entity, PropertyModel versionProperty, @Nullable Long oldVersion, Long newVersion) {
        this.entity = entity;
        versioned = true;
        this.newVersion = newVersion;
        this.oldVersion = oldVersion;
        this.versionProperty = versionProperty;
    }

    public Object entity() {
        return entity;
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

    public PropertyModel versionProperty() {
        return versionProperty;
    }

    public boolean versioned() {
        return versioned;
    }
}
