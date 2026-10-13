package dev.morphia.test.mapping.codec.harness.models;

import dev.morphia.annotations.Entity;

@Entity
public record HarnessPoint(int x, int y) {
}
