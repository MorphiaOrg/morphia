package org.mongodb.morphia.entities.version;

import org.mongodb.morphia.annotations.Entity;

@Entity
@SuppressWarnings("unused")
public class VersionedChildEntity extends AbstractVersionedBase {
    private int value;
}
