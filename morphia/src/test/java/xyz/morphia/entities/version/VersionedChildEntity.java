package xyz.morphia.entities.version;

import xyz.morphia.annotations.Entity;

@Entity
@SuppressWarnings("unused")
public class VersionedChildEntity extends AbstractVersionedBase {
    private int value;
}
