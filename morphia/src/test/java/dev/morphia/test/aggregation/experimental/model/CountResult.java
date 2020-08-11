package dev.morphia.test.aggregation.experimental.model;

import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity("counts")
public class CountResult {

    @Id
    private String author;
    @AlsoLoad("value")
    private int count;

    public String getAuthor() {
        return author;
    }

    public int getCount() {
        return count;
    }
}
