package dev.morphia.geo.model;

import com.mongodb.client.model.geojson.MultiPolygon;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.utils.IndexDirection;
import org.bson.types.ObjectId;

@Entity
public final class Regions {
    @Id
    private ObjectId id;
    private String name;

    @Indexed(IndexDirection.GEO2DSPHERE)
    private MultiPolygon regions;

    Regions() {
    }

    public Regions(String name, MultiPolygon regions) {
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
    public boolean equals(Object o) {
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
        return regions.equals(regions1.regions);
    }

    @Override
    public String toString() {
        return "Regions{"
               + "name='" + name + '\''
               + ", regions=" + regions
               + '}';
    }
}
