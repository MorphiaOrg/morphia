package dev.morphia.geo;

@SuppressWarnings("UnusedDeclaration")
public final class Stores {
    private String name;
    private MultiPoint locations;

    Stores() {
    }

    Stores(final String name, final MultiPoint locations) {
        this.name = name;
        this.locations = locations;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + locations.hashCode();
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

        Stores stores = (Stores) o;

        if (!locations.equals(stores.locations)) {
            return false;
        }
        if (!name.equals(stores.name)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Stores{"
               + "name='" + name + '\''
               + ", locations=" + locations
               + '}';
    }
}
