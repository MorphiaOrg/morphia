package dev.morphia.geo;

import dev.morphia.annotations.Indexed;
import dev.morphia.utils.IndexDirection;

@SuppressWarnings("UnusedDeclaration")
public final class Regions {
    private String name;

    @Indexed(IndexDirection.GEO2DSPHERE)
    private MultiPolygon regions;

    Regions() {
    }

    public Regions(final String name, final MultiPolygon regions) {
        this.name = name;
        this.regions = regions;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + regions.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Regions regions1 = (Regions) o;

        if (!name.equals(regions1.name)) {
            return false;
        }
        if (!regions.equals(regions1.regions)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Regions{"
               + "name='" + name + '\''
               + ", regions=" + regions
               + '}';
    }
}
