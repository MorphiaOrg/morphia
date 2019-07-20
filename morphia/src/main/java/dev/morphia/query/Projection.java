package dev.morphia.query;

import dev.morphia.annotations.Entity;
import dev.morphia.internal.PathTarget;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.Mapper;
import dev.morphia.sofia.Sofia;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class Projection {
    private FindOptions options;
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
    FindOptions exclude(String... fields) {
        if (excludes == null) {
            excludes = new ArrayList<>();
        }
        excludes.addAll(List.of(fields));
        validateProjections();
        return options;
    }

    /**
     * Adds a field to the projection clause. The _id field is always included unless explicitly suppressed.
     *
     * @param fields the fields to include
     * @return this
     * @see <a href="https://docs.mongodb.com/manual/tutorial/project-fields-from-query-results/">Project Fields to Return from Query</a>
     */
    FindOptions include(String... fields) {
        if (includes == null) {
            includes = new ArrayList<>();
        }
        includes.addAll(List.of(fields));
        validateProjections();
        return options;
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
    FindOptions project(String field, ArraySlice slice) {
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
    FindOptions project(Meta meta) {
        this.meta = meta;
        validateProjections();
        return options;
    }

    public FindOptions knownFields() {
        knownFields = true;
        return options;
    }

    public boolean isIncluding() {
        return includes != null && !includes.isEmpty();
    }

    public Document map(final Mapper mapper, final Class clazz) {
        if (includes != null || excludes != null) {
            return project(mapper, clazz);
        } else if (arrayField != null && slice != null) {
            return slice(mapper, clazz);
        } else if (meta != null) {
            return meta(mapper, clazz);
        } else if (Boolean.TRUE.equals(knownFields)) {
            return knownFields(mapper, clazz);
        }

        return null;
    }

    private Document project(final Mapper mapper, final Class clazz) {
        Document projection = new Document();
        iterate(mapper, projection, clazz, includes, 1);
        iterate(mapper, projection, clazz, excludes, 0);

        final MappedClass mc = mapper.getMappedClass(clazz);
        Entity entityAnnotation = mc.getEntityAnnotation();

        if (isIncluding() && entityAnnotation != null && entityAnnotation.useDiscriminator()) {
            projection.put(mapper.getOptions().getDiscriminatorField(), 1);
        }

        return projection;
    }

    private void iterate(Mapper mapper, final Document projection, final Class clazz, final List<String> fields, final int include) {
        if (fields != null) {
            for (final String field : fields) {
                projection.put(new PathTarget(mapper, mapper.getMappedClass(clazz), field).translatedPath(), include);
            }
        }
    }

    private Document slice(final Mapper mapper, final Class clazz) {
        String fieldName = new PathTarget(mapper, mapper.getMappedClass(clazz), arrayField).translatedPath();
        return new Document(fieldName, slice.toDatabase());
    }

    private Document meta(final Mapper mapper, final Class clazz) {
        String fieldName = new PathTarget(mapper, clazz, meta.getField(), false).translatedPath();
        return new Document(fieldName, meta.toDatabase());
    }

    private void validateProjections() {
        if((includes != null || excludes != null)&& ( slice != null || meta != null)) {
            throw new ValidationException(Sofia.mixedModeProjections());
        }
        if(slice != null && (includes != null || excludes != null || meta != null)) {
            throw new ValidationException(Sofia.mixedModeProjections());
        }
        if(meta != null && (includes != null || excludes != null || slice != null)) {
            throw new ValidationException(Sofia.mixedModeProjections());
        }
        if (includes != null && excludes != null) {
            if (excludes.size() > 1 || !"_id".equals(excludes.get(0))) {
                throw new ValidationException(Sofia.mixedProjections());
            }
        }
    }

    private Document knownFields(final Mapper mapper, final Class clazz) {
        Document projection = new Document();
        mapper.getMappedClass(clazz).getFields()
              .stream()
              .map(mf -> new PathTarget(mapper, mapper.getMappedClass(clazz), mf.getMappedFieldName()).translatedPath())
              .forEach(name -> projection.put(name, 1));

        return projection;
    }
}
