/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.morphia.internal;

import dev.morphia.annotations.Serialized;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.query.ValidationException;

import java.util.Iterator;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static dev.morphia.internal.MorphiaUtils.join;

/**
 * @since 1.3
 * @morphia.internal
 */
@SuppressWarnings("deprecation")
public class PathTarget {
    private final List<String> segments;
    private boolean validateNames;
    private int position;
    private Mapper mapper;
    private MappedClass context;
    private MappedClass root;
    private MappedField target;
    private boolean resolved;

    /**
     * Creates a resolution context for the given root and path.
     *
     * @param mapper mapper
     * @param root root
     * @param path path
     */
    public PathTarget(final Mapper mapper, final MappedClass root, final String path) {
        this(mapper, root, path, true);
    }

    public <T> PathTarget(final Mapper mapper, final Class<T> clazz, final String field) {
        this(mapper, mapper.getMappedClass(clazz), field, true);
    }

    public <T> PathTarget(final Mapper mapper, final Class<T> clazz, final String field, final boolean validateNames) {
        this(mapper, mapper.getMappedClass(clazz), field, validateNames);
    }

    /**
     * Creates a resolution context for the given root and path.
     *
     * @param mapper mapper
     * @param root root
     * @param path path
     */
    public PathTarget(final Mapper mapper, final MappedClass root, final String path, boolean validateNames) {
        segments = asList(path.split("\\."));
        this.root = root;
        this.mapper = mapper;
        this.validateNames = validateNames;
        resolved = path.startsWith("$");
    }

    private boolean hasNext() {
        return position < segments.size();
    }

    /**
     * Returns the translated path for this context.  If validation is disabled, that path could be the same as the initial value.
     *
     * @return the translated path
     */
    public String translatedPath() {
        if (!resolved) {
            resolve();
        }
        return join(segments, '.');
    }

    /**
     * Returns the MappedField found at the end of a path.  May be null if the path is invalid and validation is disabled.
     *
     * @return the field
     */
    public MappedField getTarget() {
        if (!resolved) {
            resolve();
        }
        return target;
    }

    String next() {
        return segments.get(position++);
    }

    private void resolve() {
        context = this.root;
        position = 0;
        MappedField field = null;
        while (context != null && hasNext()) {
            String segment = next();

            if ("$".equals(segment) || segment.matches("[0-9]+")) {  // array operator
                if (!hasNext()) {
                    break;
                }
                segment = next();
            }
            field = resolveField(segment);

            if (field != null) {
                if (hasNext() && (field.isReference() || field.hasAnnotation(Serialized.class))) {
                    failValidation();
                }
                translate(field.getNameToStore());
                if (field.isMap() && hasNext()) {
                    next();  // consume the map key segment
                }
            } else {
                if (validateNames) {
                    failValidation();
                }
            }
        }
        target = field;
        resolved = true;
    }

    private void failValidation() {
        throw new ValidationException(format("Could not resolve path '%s' against '%s'.", join(segments, '.'),
                                             root.getClazz().getName()));
    }

    private void translate(final String nameToStore) {
        segments.set(position - 1, nameToStore);
    }

    private MappedField resolveField(final String segment) {
        MappedField mf = context.getMappedField(segment);
        if (mf == null) {
            mf = context.getMappedFieldByJavaField(segment);
        }
        if (mf == null) {
            Iterator<MappedClass> subTypes = mapper.getSubTypes(context).iterator();
            while (mf == null && subTypes.hasNext()) {
                context = subTypes.next();
                mf = resolveField(segment);
            }
        }

        if (mf != null) {
            context = mapper.getMappedClass(mf.getSubClass() != null ? mf.getSubClass() : mf.getConcreteType());
        }
        return mf;
    }

    @Override
    public String toString() {
        return String.format("PathTarget{root=%s, segments=%s, target=%s}", root.getClazz().getSimpleName(), segments, target);
    }
}
