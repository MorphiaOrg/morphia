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

import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.codec.pojo.EntityModel;
import dev.morphia.mapping.codec.pojo.PropertyModel;
import dev.morphia.query.ValidationException;
import dev.morphia.sofia.Sofia;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static java.util.Arrays.asList;

/**
 * @morphia.internal
 * @since 1.3
 */
@MorphiaInternal
public class PathTarget {
    private final List<String> segments;
    private final boolean validateNames;
    private int position;
    private final Mapper mapper;
    private final EntityModel root;
    private EntityModel context;
    private PropertyModel target;
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
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PathTarget(Mapper mapper, @Nullable EntityModel root, String path, boolean validateNames) {
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
    public <T> PathTarget(Mapper mapper, @Nullable Class<T> type, String path) {
        this(mapper, type != null && mapper.isMappable(type) ? mapper.getEntityModel(type) : null, path, true);
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
    public <T> PathTarget(Mapper mapper, @Nullable Class<T> type, String path, boolean validateNames) {
        this(mapper, type != null && mapper.isMappable(type) ? mapper.getEntityModel(type) : null, path, validateNames);
    }

    /**
     * @return the Mapper
     * @since 2.3
     */
    public Mapper mapper() {
        return mapper;
    }

    public EntityModel root() {
        return root;
    }

    /**
     * Returns the translated path for this context. If validation is disabled, that path could be the same as the initial value.
     *
     * @return the translated path
     */
    public String translatedPath() {
        if (!resolved) {
            resolve();
        }
        StringJoiner joiner = new StringJoiner(".");
        segments.forEach(joiner::add);
        return joiner.toString();
    }

    /**
     * Returns the PropertyModel found at the end of a path. May be null if the path is invalid and validation is disabled.
     *
     * @return the field
     */
    @Nullable
    public PropertyModel target() {
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

    private void failValidation(String pathElement) {
        resolved = true;
        throw new ValidationException(Sofia.invalidPathTarget(translatedPath(), root.getType().getName(), pathElement));
    }

    private void resolve() {
        context = this.root;
        position = 0;
        PropertyModel property = null;
        while (!resolved && hasNext()) {
            String segment = next();

            // array operator
            if ("$".equals(segment) || (segment.startsWith("$[") && segment.endsWith("]")) || segment.matches("[0-9]+")) {
                if (!hasNext()) {
                    break;
                }
                segment = next();
            }
            property = resolveProperty(segment);

            if (property != null) {
                if (hasNext() && property.isReference()) {
                    failValidation(segment);
                }
                translate(property.getMappedName());
                if (property.isMap()) {
                    resolved = true;
                }
            } else if (root != null && segment.equals(root.getDiscriminatorKey())) {
                translate(segment);
            } else {
                if (validateNames) {
                    failValidation(segment);
                }
            }
        }
        target = property;
        resolved = true;
    }

    private void translate(String nameToStore) {
        segments.set(position - 1, nameToStore);
    }

    @Nullable
    private PropertyModel resolveProperty(String segment) {
        if (context != null) {
            PropertyModel model = context.getProperty(segment);
            if (model == null) {
                Iterator<EntityModel> subTypes = context.getSubtypes().iterator();
                while (model == null && subTypes.hasNext()) {
                    context = subTypes.next();
                    model = resolveProperty(segment);
                }
            }

            if (model != null) {
                context = mapper.tryGetEntityModel(model.getNormalizedType()).orElse(null);
            }
            return model;
        } else {
            return null;
        }
    }

    String next() {
        return segments.get(position++);
    }
}
