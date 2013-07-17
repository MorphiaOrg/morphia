package com.google.code.morphia.utils;


public enum IndexDirection {
    ASC(1),
    DESC(-1),
    GEO2D("2d");

    private final Object direction;

    IndexDirection(final Object o) {
        direction = o;
    }

    public Object toIndexValue() {
        return direction;
    }
}