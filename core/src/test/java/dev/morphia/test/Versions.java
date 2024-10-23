package dev.morphia.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import dev.morphia.sofia.Sofia;

import org.semver4j.Semver;
import org.testcontainers.utility.DockerImageName;

public enum Versions {
    Version7 {
        @Override
        Semver version() {
            return Semver.of(7, 0, 0).withBuild("latest");
        }
    },
    Version6 {
        @Override
        Semver version() {
            return Semver.of(6, 0, 0).withBuild("latest");
        }
    },
    Version5 {
        @Override
        Semver version() {
            return Semver.of(5, 0, 0).withBuild("latest");
        }

    },
    Version44 {
        @Override
        Semver version() {
            return Semver.of(4, 4, 19);
        }
    },
    Version42 {
        @Override
        Semver version() {
            return Semver.of(4, 2, 24);
        }
    },
    Version40 {
        @Override
        Semver version() {
            return Semver.of(4, 0, 28);
        }
    };

    public static Versions find(Semver target) {
        for (Versions value : values()) {
            if (value.matches(target)) {
                return value;
            }
        }
        return null;
    }

    private boolean matches(Semver target) {
        boolean latest = version().getBuild().contains("latest");
        return target.getMajor() == version().getMajor()
                && (latest || target.getMinor() == version().getMinor());
    }

    public static Versions latest() {
        return values()[0];
    }

    public static Versions bestMatch(String mongo) {
        List<String> parts = new ArrayList<>(Arrays.asList(mongo.split("\\.")));
        while (parts.size() < 3) {
            parts.add("0");
        }
        StringJoiner joiner = new StringJoiner(".");
        for (String part : parts) {
            joiner.add(part);
        }
        Semver suggested = Semver.parse(joiner.toString());

        return Arrays.stream(values())
                .filter(it -> it.version().getMajor() == suggested.getMajor()
                        && it.version().getMinor() == suggested.getMinor())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(Sofia.unknownMongoDbVersion(suggested)));
    }

    final DockerImageName dockerImage() {
        Semver version = version();
        String tag;
        if (version.getBuild().contains("latest")) {
            tag = String.valueOf(version.getMajor());
        } else {
            tag = version.toString();
        }
        return DockerImageName.parse("mongo:" + tag)
                .asCompatibleSubstituteFor("mongo");
    }

    abstract Semver version();

    @Override
    public String toString() {
        Semver version = version();

        var numbers = new StringJoiner(".");
        numbers.add(String.valueOf(version.getMajor()));
        if (version.getMinor() != 0 || version.getPatch() != 0) {
            numbers.add(String.valueOf(version().getMinor()))
                    .add(String.valueOf(version().getPatch()));
        }
        return numbers.toString();
    }
}
