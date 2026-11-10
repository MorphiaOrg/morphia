package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

/**
 * A polymorphic root with a string id.
 */
@Entity("harness_tickets")
public class HarnessKeyedTicket {
    @Id
    private String id;
    private String title;

    public HarnessKeyedTicket() {
    }

    protected HarnessKeyedTicket(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public static HarnessKeyedTicket sample() {
        return new HarnessKeyedBug("BUG-1", "crash", 2);
    }
}
