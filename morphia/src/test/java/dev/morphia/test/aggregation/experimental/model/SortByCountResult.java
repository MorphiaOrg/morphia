package dev.morphia.test.aggregation.experimental.model;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
public class SortByCountResult {
    @Id
    private String id;
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SortByCountResult{"
               + "id=" + id
               + ", count=" + count
               + '}';
    }
}
