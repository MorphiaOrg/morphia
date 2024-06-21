package dev.morphia.test.util;

import dev.morphia.query.FindOptions;
import dev.morphia.test.DriverVersion;
import dev.morphia.test.ServerVersion;

import static dev.morphia.test.ServerVersion.ANY;

public class ActionTestOptions {
    private FindOptions findOptions;

    private DriverVersion minDriver = DriverVersion.v41;

    private boolean orderMatters = true;

    private boolean removeIds = false;

    private ServerVersion serverVersion = ANY;

    private boolean skipActionCheck;

    private boolean skipDataCheck = false;

    public FindOptions findOptions() {
        return findOptions;
    }

    public ActionTestOptions findOptions(FindOptions findOptions) {
        this.findOptions = findOptions;
        return this;
    }

    public DriverVersion minDriver() {
        return minDriver;
    }

    public ActionTestOptions minDriver(DriverVersion minDriver) {
        this.minDriver = minDriver;
        return this;
    }

    public boolean orderMatters() {
        return orderMatters;
    }

    public ActionTestOptions orderMatters(boolean orderMatters) {
        this.orderMatters = orderMatters;
        return this;
    }

    public boolean removeIds() {
        return removeIds;
    }

    public ActionTestOptions removeIds(boolean removeIds) {
        this.removeIds = removeIds;
        return this;
    }

    public ServerVersion serverVersion() {
        return serverVersion;
    }

    public ActionTestOptions serverVersion(ServerVersion serverVersion) {
        this.serverVersion = serverVersion;
        return this;
    }

    public ActionTestOptions skipActionCheck(boolean skipActionCheck) {
        this.skipActionCheck = skipActionCheck;
        return this;
    }

    public boolean skipActionCheck() {
        return skipActionCheck;
    }

    public boolean skipDataCheck() {
        return skipDataCheck;
    }

    public ActionTestOptions skipDataCheck(boolean skipDataCheck) {
        this.skipDataCheck = skipDataCheck;
        return this;
    }
}
