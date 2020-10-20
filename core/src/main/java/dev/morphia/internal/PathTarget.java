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

import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.FieldModel;
import dev.morphia.query.ValidationException;
import dev.morphia.sofia.Sofia;

import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import static java.util.Arrays.asList;

/**
 * @morphia.internal
 * @since 1.3
 */
public class PathTarget {
    private final List<String> segments;
    private final boolean validateNames;
    private int position;
    private final Mapper mapper;
    private final EntityModel root;
    private EntityModel context;
    private FieldModel target;
    private boolean resolved;

    /**
     * Creates a resolution context for the given root and path.
     *
     * @param mapper mapper
     * @param root   root
     * @param path   path
     */
    public PathTarget(Mapper mapper, EntityModel root, String path) {
        this(mapper, root, path, true);
    }

    /**
     * Creates a resolution context for the given root and path.
     *
     * @param mapper        mapper
     * @param root          root
     * @param path          path
     * @param validateNames true if names should be validated
     */
    public PathTarget(Mapper mapper, EntityModel root, String path, boolean validateNames) {
        segments = asList(path.split("\\."));
        this.root = root;
        this.mapper = mapper;
        this.validateNames = validateNames;
        resolved = path.startsWith("$");
    }

    /**
     * Creates a resolution context for the given root and path.
     *
     * @param mapper mapper
     * @param type   the root type
     * @param path   the path
     * @param <T>    the root type
     */
    public <T> PathTarget(Mapper mapper, Class<T> type, String path) {
        this(mapper, mapper.getEntityModel(type), path, true);
    }

    /**
     * Creates a resolution context for the given root and path.
     *
     * @param mapper        mapper
     * @param type          the root type
     * @param path          the path
     * @param validateNames true if names should be validated
     * @param <T>           the root type
     */
    public <T> PathTarget(Mapper mapper, Class<T> type, String path, boolean validateNames) {
        this(mapper, mapper.getEntityModel(type), path, validateNames);
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
        StringJoiner joiner = new StringJoiner(".");
        segments.forEach(s -> joiner.add(s));
        return joiner.toString();
    }

    /**
     * Returns the MappedField found at the end of a path.  May be null if the path is invalid and validation is disabled.
     *
     * @return the field
     */
    public FieldModel getTarget() {
        if (!resolved) {
            resolve();
        }
        return target;
    }

    @Override
    public String toString() {
        return String.format("PathTarget{root=%s, segments=%s, target=%s}", root.getType().getSimpleName(), segments, target);
    }

    private boolean hasNext() {
        return position < segments.size();
    }

    private void resolve() {
        context = this.root;
        position = 0;
        FieldModel field = null;
        while (hasNext()) {
            String segment = next();

            // array operator
            if ("$".equals(segment) || (segment.startsWith("$[") && segment.endsWith("]")) || segment.matches("[0-9]+")) {
                if (!hasNext()) {
                    break;
                }
                segment = next();
            }
            field = resolveField(segment);

            if (field != null) {
                if (hasNext() && field.isReference()) {
                    failValidation();
                }
                translate(field.getMappedName());
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
        resolved = true;
        throw new ValidationException(Sofia.invalidPathTarget(translatedPath(), root.getType().getName()));
    }

    private void translate(String nameToStore) {
        segments.set(position - 1, nameToStore);
    }

    private FieldModel resolveField(String segment) {
        if (context != null) {
            FieldModel mf = context.getField(segment);
            if (mf == null) {
                Iterator<EntityModel> subTypes = context.getSubtypes().iterator();
                while (mf == null && subTypes.hasNext()) {
                    context = subTypes.next();
                    mf = resolveField(segment);
                }
            }

            if (mf != null) {
                context = mapper.getEntityModel(mf.getNormalizedType());
            }
            return mf;
        } else {
            return null;
        }
    }

    String next() {
        return segments.get(position++);
    }
}
