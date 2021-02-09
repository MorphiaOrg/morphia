package dev.morphia.query;

import com.mongodb.lang.Nullable;
import dev.morphia.annotations.Entity;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a query projection
 */
public class Projection {
    private final FindOptions options;
    private List<String> includes;
    private List<String> excludes;
    private String arrayField;
    private ArraySlice slice;
    private Meta meta;
    private Boolean knownFields;

    Projection(FindOptions options) {
        this.options = options;
    }

    /**
     * Adds a field to the projection clause. The _id field is always included unless explicitly suppressed.
     *
     * @param fields the fields to exclude
     * @return this
     * @see <a href="https://docs.mongodb.com/manual/tutorial/project-fields-from-query-results/">Project Fields to Return from Query</a>
     */
    public FindOptions exclude(String... fields) {
        if (excludes == null) {
            excludes = new ArrayList<>();
        }
        excludes.addAll(List.of(fields));
        validateProjections();
        return options;
    }

    private void validateProjections() {
        if ((includes != null || excludes != null) && (slice != null || meta != null)) {
            throw new ValidationException(Sofia.mixedModeProjections());
        }
        if (slice != null && meta != null) {
            throw new ValidationException(Sofia.mixedModeProjections());
        }
        if (includes != null && excludes != null) {
            if (excludes.size() > 1 || !"_id".equals(excludes.get(0))) {
                throw new ValidationException(Sofia.mixedProjections());
            }
        }
    }

    /**
     * Adds a field to the projection clause. The _id field is always included unless explicitly suppressed.
     *
     * @param fields the fields to include
     * @return this
     * @see <a href="https://docs.mongodb.com/manual/tutorial/project-fields-from-query-results/">Project Fields to Return from Query</a>
     */
    public FindOptions include(String... fields) {
        if (includes == null) {
            includes = new ArrayList<>();
        }
        includes.addAll(List.of(fields));
        validateProjections();
        return options;
    }

    /**
     * Configure the project to only return known, mapped fields
     *
     * @return this
     */
    public FindOptions knownFields() {
        knownFields = true;
        return options;
    }

    /**
     * Converts this to Document form
     *
     * @param mapper the Mapper to use
     * @param type   the entity type
     * @return this
     */
    @Nullable
    public Document map(Mapper mapper, Class<?> type) {
        if (includes != null || excludes != null) {
            return project(mapper, type);
        } else if (arrayField != null && slice != null) {
            return slice(mapper, type);
        } else if (meta != null) {
            return meta(mapper, type);
        } else if (Boolean.TRUE.equals(knownFields)) {
            return knownFields(mapper, type);
        }

        return null;
    }

    private void iterate(Mapper mapper, Document projection, Class<?> clazz, @Nullable List<String> fields,
                         int include) {
        if (fields != null) {
            for (String field : fields) {
                projection.put(new PathTarget(mapper, mapper.getEntityModel(clazz), field).translatedPath(), include);
            }
        }
    }

    private Document knownFields(Mapper mapper, Class<?> clazz) {
        Document projection = new Document();
        mapper.getEntityModel(clazz).getProperties()
              .stream()
              .map(mf -> new PathTarget(mapper, mapper.getEntityModel(clazz), mf.getMappedName()).translatedPath())
              .forEach(name -> projection.put(name, 1));

        return projection;
    }

    private Document meta(Mapper mapper, Class<?> clazz) {
        String fieldName = new PathTarget(mapper, clazz, meta.getField(), false).translatedPath();
        return new Document(fieldName, meta.toDatabase().get(meta.getField()));
    }

    private Document project(Mapper mapper, Class<?> clazz) {
        Document projection = new Document();
        iterate(mapper, projection, clazz, includes, 1);
        iterate(mapper, projection, clazz, excludes, 0);

        final EntityModel model = mapper.getEntityModel(clazz);
        Entity entityAnnotation = model.getEntityAnnotation();

        if (isIncluding() && entityAnnotation != null && entityAnnotation.useDiscriminator()) {
            projection.put(mapper.getOptions().getDiscriminatorKey(), 1);
        }

        return projection;
    }

    private Document slice(Mapper mapper, Class<?> clazz) {
        String fieldName = new PathTarget(mapper, mapper.getEntityModel(clazz), arrayField).translatedPath();
        return new Document(fieldName, slice.toDatabase());
    }

    boolean isIncluding() {
        return includes != null && !includes.isEmpty();
    }

    /**
     * Adds an sliced array field to a projection.
     *
     * @param field the field to project
     * @param slice the options for projecting an array field
     * @return this
     * @mongodb.driver.manual /reference/operator/projection/slice/ $slice
     * @see <a href="https://docs.mongodb.com/manual/tutorial/project-fields-from-query-results/">Project Fields to Return from Query</a>
     */
    public FindOptions project(String field, ArraySlice slice) {
        this.arrayField = field;
        this.slice = slice;
        validateProjections();
        return options;
    }

    /**
     * Adds a metadata field to a projection.
     *
     * @param meta the metadata option for projecting
     * @return this
     * @mongodb.driver.manual reference/operator/projection/meta/ $meta
     * @see <a href="https://docs.mongodb.com/manual/tutorial/project-fields-from-query-results/">Project Fields to Return from Query</a>
     */
    public FindOptions project(Meta meta) {
        this.meta = meta;
        validateProjections();
        return options;
    }
}
