package dev.morphia.test.query.filters;

import dev.morphia.query.FindOptions;
import dev.morphia.test.DriverVersion;
import dev.morphia.test.ServerVersion;

import static dev.morphia.test.ServerVersion.ANY;

public class QueryTestOptions {
    private FindOptions findOptions;

    private DriverVersion minDriver = DriverVersion.v41;

    private boolean orderMatters = true;

    private boolean removeIds = false;

    private ServerVersion serverVersion = ANY;

    private boolean skipDataCheck = false;

    public QueryTestOptions() {
        serverVersion = ANY;
    }

    public FindOptions findOptions() {
        return findOptions;
    }

    public QueryTestOptions findOptions(FindOptions findOptions) {
        this.findOptions = findOptions;
        return this;
    }

    public DriverVersion minDriver() {
        return minDriver;
    }

    public QueryTestOptions minDriver(DriverVersion minDriver) {
        this.minDriver = minDriver;
        return this;
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
