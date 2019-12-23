package dev.morphia.aggregation.experimental.model;

import dev.morphia.aggregation.experimental.AggregationTest;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

@Entity
public class BucketAutoResult {

    @Id
    private MinMax id;
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public MinMax getId() {
        return id;
    }

    public void setId(final MinMax id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "BucketAutoResult{"
               + "id=" + id
               + ", count=" + count
               + '}';
    }

    public static class MinMax {
        private int min;
        private int max;

        public int getMax() {
            return max;
        }

        public void setMax(final int max) {
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public void setMin(final int min) {
            this.min = min;
        }

        @Override
        public String toString() {
            return "MinMax{"
                   + "min=" + min
                   + ", max=" + max
                   + '}';
        }
    }
}
