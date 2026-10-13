package dev.morphia.test.mapping.codec.harness.models;

import java.util.List;

import dev.morphia.annotations.Entity;

import org.bson.types.ObjectId;

/**
 * Binds the superclass's type variable, so {@code value} must be mapped as {@code List<String>}.
 */
@Entity("harness_boxes")
public class HarnessStringListBox extends HarnessBox<List<String>> {
    public HarnessStringListBox() {
    }

    public static HarnessStringListBox sample() {
        HarnessStringListBox entity = new HarnessStringListBox();
        entity.id = new ObjectId("65000000000000000000000b");
        entity.value = List.of("boxed", "strings");
        return entity;
    }
}
