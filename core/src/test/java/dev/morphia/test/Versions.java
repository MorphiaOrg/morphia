package dev.morphia.test;

import java.util.Arrays;
import java.util.StringJoiner;

import com.github.zafarkhaja.semver.Version;

import dev.morphia.sofia.Sofia;

public enum Versions {
    Version6 {
        @Override
        String dockerImage() {
            return "mongo:6";
        }

        @Override
        Version version() {
            return Version.forIntegers(6).setBuildMetadata("latest");
        }
    },
    Version5 {
        @Override
        String dockerImage() {
            return "mongo:5";
        }

        @Override
        Version version() {
            return Version.forIntegers(5).setBuildMetadata("latest");
        }

    },
    Version44 {
        @Override
        String dockerImage() {
            return "mongo:4.4.19-focal";
        }

        @Override
        Version version() {
            return Version.forIntegers(4, 4, 19);
        }
    },
    Version42 {
        @Override
        String dockerImage() {
            return "mongo:4.2.24";
        }

        @Override
        Version version() {
            return Version.forIntegers(4, 2, 24);
        }
    },
    Version40 {
        @Override
        String dockerImage() {
            return "mongo:4.0.28-xenial";
        }

        @Override
        Version version() {
            return Version.forIntegers(4, 0, 28);
        }
    };

    public static Versions find(Version target) {
        for (Versions value : values()) {
            if (value.matches(target)) {
                return value;
            }
        }
        return null;
    }

    private boolean matches(Version target) {
        boolean latest = version().getBuildMetadata().equals("latest");
        return target.getMajorVersion() == version().getMajorVersion()
                && (latest || target.getMinorVersion() == version().getMinorVersion());
    }

    public static Version latest() {
        return values()[0].version();
    }

    public static Versions bestMatch(Version suggested) {

        return Arrays.stream(values())
                .filter(it -> it.version().getMajorVersion() == suggested.getMajorVersion()
                        && it.version().getMinorVersion() == suggested.getMinorVersion())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(Sofia.unknownMongoDbVersion(suggested)));
    }

    String dockerImage() {
        return "mongo:" + version().toString();
    }

    abstract Version version();

    @Override
    public String toString() {
        Version version = version();

        var numbers = new StringJoiner(".");
        numbers.add(version.getMajorVersion() + "");
        if (version.getMinorVersion() != 0 || version.getPatchVersion() != 0) {
            numbers.add(version().getMinorVersion() + "");
            numbers.add(version().getPatchVersion() + "");
        }
        return numbers.toString();
    }
}
