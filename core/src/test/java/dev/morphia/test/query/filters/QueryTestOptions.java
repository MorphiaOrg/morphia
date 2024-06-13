package dev.morphia.test.query.filters;

import dev.morphia.test.ServerVersion;

import static dev.morphia.test.ServerVersion.ANY;

public class QueryTestOptions {
    private boolean orderMatters = true;

    private boolean removeIds = false;

    private ServerVersion serverVersion = ANY;

    private boolean skipDataCheck = false;

    public QueryTestOptions() {
        serverVersion = ANY;
    }

    public boolean orderMatters() {
        return orderMatters;
    }

    public QueryTestOptions orderMatters(boolean orderMatters) {
        this.orderMatters = orderMatters;
        return this;
    }

    public boolean removeIds() {
        return removeIds;
    }

    public QueryTestOptions removeIds(boolean removeIds) {
        this.removeIds = removeIds;
        return this;
    }

    public ServerVersion serverVersion() {
        return serverVersion;
    }

    public QueryTestOptions serverVersion(ServerVersion serverVersion) {
        this.serverVersion = serverVersion;
        return this;
    }

    public boolean skipDataCheck() {
        return skipDataCheck;
    }

    public QueryTestOptions skipDataCheck(boolean skipDataCheck) {
        this.skipDataCheck = skipDataCheck;
        return this;
    }
}
