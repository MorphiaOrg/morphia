package dev.morphia.test.models.records;

import java.util.List;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;

import org.bson.types.ObjectId;

/**
 * A record entity in a package listed in {@code morphia.packages}, so critter-maven generates its model ahead of time.
 */
@Entity("aot_record_people")
public record AotRecordPerson(@Id ObjectId id, String name, @Property("years") int age, List<String> nicknames) {
}
