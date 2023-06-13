package dev.morphia.test.models.errors.invalidVersion;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Version;

@Entity
public class InvalidVersionUse {
    @Id
    private String id;
    @Version
    private long version1;
    @Version
    private long version2;

}
