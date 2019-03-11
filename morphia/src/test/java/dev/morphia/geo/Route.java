package dev.morphia.geo;

import dev.morphia.annotations.Indexed;
import dev.morphia.utils.IndexDirection;

public final class Route {
    private String name;

    @Indexed(IndexDirection.GEO2DSPHERE)
    private LineString route;

    @SuppressWarnings("UnusedDeclaration")
        // needed by Morphia
    Route() {
    }

    public Route(final String name, final LineString route) {
        this.name = name;
        this.route = route;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (route != null ? route.hashCode() : 0);
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

        Route route1 = (Route) o;

        if (name != null ? !name.equals(route1.name) : route1.name != null) {
            return false;
        }
        if (route != null ? !route.equals(route1.route) : route1.route != null) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Route{"
               + "name='" + name + '\''
               + ", route=" + route
               + '}';
    }
}
