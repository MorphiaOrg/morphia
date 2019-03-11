package dev.morphia.geo;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.utils.IndexDirection;

import java.util.Arrays;

@SuppressWarnings("unused")
public class PlaceWithLegacyCoords {
    @Id
    private ObjectId id;
    @Indexed(IndexDirection.GEO2D)
    private double[] location = new double[2];
    private String name;

    public PlaceWithLegacyCoords(final double[] location, final String name) {
        this.location = location;
        this.name = name;
    }

    PlaceWithLegacyCoords() {
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(location);
        result = 31 * result + name.hashCode();
        return result;
    }

    // equals(), hashCode() and toString() all needed for testing
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PlaceWithLegacyCoords place = (PlaceWithLegacyCoords) o;

        if (!Arrays.equals(location, place.location)) {
            return false;
        }
        if (!name.equals(place.name)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Place{"
               + "location=" + Arrays.toString(location)
               + ", name='" + name + '\''
               + '}';
    }
}
