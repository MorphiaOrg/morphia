package org.mongodb.morphia.geo;

import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.utils.IndexDirection;

public class City {
    @Indexed(IndexDirection.GEO2DSPHERE)
    private Point location;
    private String name;

    //needed for Morphia serialisation
    @SuppressWarnings("unused")
    public City() {
    }

    public City(final String name, final Point location) {
        this.name = name;
        this.location = location;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        City city = (City) o;

        if (!location.equals(city.location)) {
            return false;
        }
        if (!name.equals(city.name)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = location.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "City{"
               + "location=" + location
               + ", name='" + name + '\''
               + '}';
    }
}
