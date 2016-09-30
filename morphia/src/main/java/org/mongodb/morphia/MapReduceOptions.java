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

package org.mongodb.morphia;

import com.mongodb.DBCollection;
import com.mongodb.MapReduceCommand;
import com.mongodb.MapReduceCommand.OutputType;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.QueryException;
import org.mongodb.morphia.utils.Assert;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MapReduceOptions<T> {
    private String outputDB;
    private String outputCollection;
    private String map;
    private String reduce;
    private OutputType outputType;
    private Query query;
    private String finalize;
    private ReadPreference readPreference;
    private int limit;
    private long maxTimeMS;
    private Map<String, Object> scope;
    private boolean jsMode;
    private boolean verbose;
    private boolean bypassDocumentValidation;
    private Collation collation;
    private Class<T> resultType;

    public MapReduceOptions<T> bypassDocumentValidation(final Boolean bypassDocumentValidation) {
        this.bypassDocumentValidation = bypassDocumentValidation;
        return this;
    }

    public MapReduceOptions<T> collation(final Collation collation) {
        this.collation = collation;
        return this;
    }

    public MapReduceOptions<T> finalize(final String finalize) {
        this.finalize = finalize;
        return this;
    }

    public Boolean getBypassDocumentValidation() {
        return bypassDocumentValidation;
    }

    public Collation getCollation() {
        return collation;
    }

    public String getFinalize() {
        return finalize;
    }

    public Boolean getJsMode() {
        return jsMode;
    }

    public int getLimit() {
        return limit;
    }

    public String getMap() {
        return map;
    }

    public long getMaxTimeMS() {
        return maxTimeMS;
    }

    public String getOutputCollection() {
        return outputCollection;
    }

    public String getOutputDB() {
        return outputDB;
    }

    public OutputType getOutputType() {
        return outputType;
    }

    public Query getQuery() {
        return query;
    }

    public ReadPreference getReadPreference() {
        return readPreference;
    }

    public String getReduce() {
        return reduce;
    }

    public Class<T> getResultType() {
        return resultType;
    }

    public Map<String, Object> getScope() {
        return scope;
    }

    public Boolean getVerbose() {
        return verbose;
    }

    public MapReduceOptions<T> jsMode(final Boolean jsMode) {
        this.jsMode = jsMode;
        return this;
    }

    public MapReduceOptions<T> limit(final int limit) {
        this.limit = limit;
        return this;
    }

    public MapReduceOptions<T> map(final String map) {
        Assert.parametersNotNull("map", map);
        Assert.parameterNotEmpty("map", map);
        this.map = map;
        return this;
    }

    public MapReduceOptions<T> maxTimeMS(final long maxTimeMS) {
        this.maxTimeMS = maxTimeMS;
        return this;
    }

    public MapReduceOptions<T> outputCollection(final String outputCollection) {
        this.outputCollection = outputCollection;
        return this;
    }

    public MapReduceOptions<T> outputDB(final String outputDB) {
        this.outputDB = outputDB;
        return this;
    }

    public MapReduceOptions<T> outputType(final OutputType outputType) {
        this.outputType = outputType;
        return this;
    }

    public MapReduceOptions<T> query(final Query query) {
        Assert.parametersNotNull("query", query);
        this.query = query;
        return this;
    }

    public MapReduceOptions<T> readPreference(final ReadPreference readPreference) {
        this.readPreference = readPreference;
        return this;
    }

    public MapReduceOptions<T> reduce(final String reduce) {
        Assert.parametersNotNull("reduce", reduce);
        Assert.parameterNotEmpty("reduce", reduce);
        this.reduce = reduce;
        return this;
    }

    public MapReduceOptions<T> resultType(final Class<T> resultType) {
        this.resultType = resultType;
        return this;
    }

    public MapReduceOptions<T> scope(final Map<String, Object> scope) {
        this.scope = scope;
        return this;
    }

    public MapReduceOptions<T> verbose(final Boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    MapReduceCommand toCommand() {

        if (query.getOffset() != 0 || query.getFieldsObject() != null) {
                throw new QueryException("mapReduce does not allow the offset/retrievedFields query ");
            }

        final DBCollection dbColl = query.getCollection();

        final MapReduceCommand command = new MapReduceCommand(dbColl, map, reduce, outputCollection, outputType, query.getQueryObject());
        command.setBypassDocumentValidation(bypassDocumentValidation);
        command.setCollation(collation);
        command.setFinalize(finalize);
        command.setJsMode(jsMode);
        command.setLimit(limit);
        command.setMaxTime(maxTimeMS, TimeUnit.MILLISECONDS);
        command.setOutputDB(outputDB);
        command.setReadPreference(readPreference);
        command.setScope(scope);
        command.setSort(query.getSortObject());
        command.setVerbose(verbose);

        return command;
    }
}
