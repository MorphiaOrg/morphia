/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
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

package dev.morphia;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.WriteConcern;
import com.mongodb.client.model.Collation;
import com.mongodb.lang.Nullable;

import dev.morphia.annotations.internal.MorphiaInternal;
import dev.morphia.internal.CollectionConfigurable;
import dev.morphia.internal.WriteConfigurable;
import dev.morphia.query.filters.Filter;

import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * The options to apply when updating documents in the MongoCollection
 *
 * @mongodb.driver.manual tutorial/modify-documents/ Updates
 * @mongodb.driver.manual reference/operator/update/ Update Operators
 * @mongodb.driver.manual reference/command/update/ Update Command
 * @since 1.3
 */
public class UpdateOptions extends com.mongodb.client.model.UpdateOptions
        implements WriteConfigurable<UpdateOptions>, CollectionConfigurable<UpdateOptions> {
    private WriteConcern writeConcern;
    private boolean multi;
    private String collection;

    /**
     * Adds a new array filter
     *
     * @param filter the new filter
     * @return this
     * @since 2.1
     */
    public UpdateOptions arrayFilter(Filter filter) {
        List<Bson> arrayFilters = new ArrayList<>();
        if (getArrayFilters() != null) {
            arrayFilters.addAll(getArrayFilters());
        }
        Document filterDoc = new Document(filter.getName(), filter.getValue());
        if (filter.isNot()) {
            filterDoc = new Document("$not", filterDoc);
        }
        arrayFilters.add(new Document(filter.getField(), filterDoc));
        arrayFilters(arrayFilters);

        return this;
    }

    @Override
    public UpdateOptions collection(String collection) {
        this.collection = collection;
        return this;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public String collection() {
        return collection;
    }

    /**
     * @param comment the comment
     * @return this
     * @see com.mongodb.client.model.InsertOneOptions#comment(String)
     * @since 2.3
     */
    public UpdateOptions comment(String comment) {
        super.comment(comment);
        return this;
    }

    /**
     * @param comment the comment
     * @return this
     * @see com.mongodb.client.model.InsertOneOptions#comment(BsonValue)
     * @since 2.3
     */
    public UpdateOptions comment(BsonValue comment) {
        super.comment(comment);
        return this;
    }

    /**
     * @return this
     * @see com.mongodb.client.model.UpdateOptions#let(Bson)
     * @since 2.3
     */
    @Override
    public UpdateOptions let(Bson variables) {
        super.let(variables);
        return this;
    }

    @Override
    public UpdateOptions sort(Bson sort) {
        super.sort(sort);
        return this;
    }

    /**
     * @param hint the hint to apply
     * @return this
     * @see #hint(Bson)
     * @see com.mongodb.client.model.UpdateOptions#hint(Bson)
     * @since 2.2
     */
    public UpdateOptions hint(Document hint) {
        super.hint(hint);
        return this;
    }

    /**
     * If true, sets this update to affect all matched documents.
     *
     * @param multi true for multiple updates
     * @return this
     */
    public UpdateOptions multi(boolean multi) {
        this.multi = multi;
        return this;
    }

    @Override
    public UpdateOptions upsert(boolean upsert) {
        super.upsert(upsert);
        return this;
    }

    @Override
    public UpdateOptions bypassDocumentValidation(@Nullable Boolean bypassDocumentValidation) {
        super.bypassDocumentValidation(bypassDocumentValidation);
        return this;
    }

    @Override
    public UpdateOptions collation(@Nullable Collation collation) {
        super.collation(collation);
        return this;
    }

    @Override
    public UpdateOptions arrayFilters(@Nullable List<? extends Bson> arrayFilters) {
        super.arrayFilters(arrayFilters);
        return this;
    }

    /**
     * @return true if the update should affect all entities
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public boolean multi() {
        return multi;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.2
     */
    public UpdateOptions hint(@Nullable Bson hint) {
        super.hint(hint);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return this
     * @since 2.2
     */
    public UpdateOptions hintString(@Nullable String hint) {
        super.hintString(hint);
        return this;
    }

    /**
     * Sets the write concern
     *
     * @param writeConcern the write concern
     * @return this
     */
    public UpdateOptions writeConcern(@Nullable WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }

    /**
     * The write concern to use for the insertion. By default the write concern configured for the MongoCollection instance will be used.
     *
     * @return the write concern, or null if the default will be used.
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    public WriteConcern writeConcern() {
        return writeConcern;
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public Boolean getBypassDocumentValidation() {
        return super.getBypassDocumentValidation();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public Collation getCollation() {
        return super.getCollation();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public List<? extends Bson> getArrayFilters() {
        return super.getArrayFilters();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public Bson getHint() {
        return super.getHint();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public String getHintString() {
        return super.getHintString();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public BsonValue getComment() {
        return super.getComment();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public Bson getLet() {
        return super.getLet();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public boolean isUpsert() {
        return super.isUpsert();
    }

    /**
     * @hidden
     * @morphia.internal
     */
    @MorphiaInternal
    @Override
    public String toString() {
        return super.toString();
    }
}
