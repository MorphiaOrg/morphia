package dev.morphia.test.aggregation.experimental.model;

import dev.morphia.annotations.Id;

public class BucketResult {
    @Id
    private String id;
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "BucketResult{"
               + "id="
               + id
               + ", count=" + count
               + '}';
    }
}
