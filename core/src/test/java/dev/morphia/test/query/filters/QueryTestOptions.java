package dev.morphia.test.query.filters;

import dev.morphia.test.ServerVersion;

import static dev.morphia.test.ServerVersion.ANY;

public class QueryTestOptions {
    private ServerVersion serverVersion = ANY;
    private boolean removeIds = false;
    private boolean orderMatters = true;

    public QueryTestOptions() {
        serverVersion = ANY;
    }

    public ServerVersion serverVersion() {
        return serverVersion;
    }

    public QueryTestOptions serverVersion(ServerVersion serverVersion) {
        this.serverVersion = serverVersion;
        return this;
    }

    public boolean removeIds() {
        return removeIds;
    }

    public QueryTestOptions removeIds(boolean removeIds) {
        this.removeIds = removeIds;
        return this;
    }

    public boolean orderMatters() {
        return orderMatters;
    }

    public QueryTestOptions orderMatters(boolean orderMatters) {
        this.orderMatters = orderMatters;
        return this;
    }
}
