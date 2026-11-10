package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;

@Entity
public class HarnessKeyedBug extends HarnessKeyedTicket {
    private int severity;

    public HarnessKeyedBug() {
    }

    public HarnessKeyedBug(String id, String title, int severity) {
        super(id, title);
        this.severity = severity;
    }
}
