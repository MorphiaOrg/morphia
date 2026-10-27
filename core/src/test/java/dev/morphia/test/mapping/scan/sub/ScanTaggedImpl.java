package dev.morphia.test.mapping.scan.sub;

import dev.morphia.annotations.Id;
import dev.morphia.test.mapping.scan.base.ScanTaggedChild;

import org.bson.types.ObjectId;

public class ScanTaggedImpl implements ScanTaggedChild {
    @Id
    public ObjectId id;
}
