package dev.morphia.test.records;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;

import org.bson.types.ObjectId;

/**
 * A record entity outside the packages listed in {@code morphia.packages}, so the critter mapper generates its model at runtime.
 */
@Entity("runtime_record_people")
public record RuntimeRecordPerson(@Id ObjectId id, String name, @Property("years") int age, List<String> nicknames) {
}
