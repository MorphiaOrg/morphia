package dev.morphia.aggregation.experimental.stages;

public class CurrentOp extends Stage {
    boolean allUsers;
    boolean idleConnections;
    boolean idleCursors;
    boolean idleSessions;
    boolean localOps;

    protected CurrentOp() {
        super("$currentOp");
    }

    public static CurrentOp of() {
        return new CurrentOp();
    }

    public CurrentOp allUsers(final boolean allUsers) {
        this.allUsers = allUsers;
        return this;
    }

    public CurrentOp idleConnections(final boolean idleConnections) {
        this.idleConnections = idleConnections;
        return this;
    }

    public CurrentOp idleCursors(final boolean idleCursors) {
        this.idleCursors = idleCursors;
        return this;
    }

    public CurrentOp idleSessions(final boolean idleSessions) {
        this.idleSessions = idleSessions;
        return this;
    }

    public boolean isAllUsers() {
        return allUsers;
    }

    public boolean isIdleConnections() {
        return idleConnections;
    }

    public boolean isIdleCursors() {
        return idleCursors;
    }

    public boolean isIdleSessions() {
        return idleSessions;
    }

    public boolean isLocalOps() {
        return localOps;
    }

    public CurrentOp localOps(final boolean localOps) {
        this.localOps = localOps;
        return this;
    }
}
