package dev.morphia.test.util;

import dev.morphia.UpdateOptions;
import dev.morphia.query.FindOptions;

public class ActionTestOptions {
    private FindOptions findOptions = new FindOptions();

    private String minDriver = "4.1.0";

    private boolean orderMatters = true;

    private boolean removeIds = false;

    private String serverVersion = "0.0.0";

    private boolean skipActionCheck;

    private boolean skipDataCheck = false;

    private UpdateOptions updateOptions;

    public FindOptions findOptions() {
        return findOptions;
    }

    public ActionTestOptions findOptions(FindOptions findOptions) {
        this.findOptions = findOptions;
        return this;
    }

    public String minDriver() {
        return minDriver;
    }

    public ActionTestOptions minDriver(String minDriver) {
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

    public String serverVersion() {
        return serverVersion;
    }

    public ActionTestOptions serverVersion(String serverVersion) {
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

    public UpdateOptions updateOptions() {
        return updateOptions != null ? updateOptions : new UpdateOptions();
    }

    public ActionTestOptions updateOptions(UpdateOptions updateOptions) {
        this.updateOptions = updateOptions;
        return this;
    }
}
