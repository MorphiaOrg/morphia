package dev.morphia.entities.version;

import dev.morphia.annotations.Entity;

@Entity
@SuppressWarnings("unused")
public class VersionedChildEntity extends AbstractVersionedBase {
    private int value;
}
