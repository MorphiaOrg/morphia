package dev.morphia.test;

import java.util.Arrays;
import java.util.List;

import com.github.zafarkhaja.semver.Version;

import dev.morphia.sofia.Sofia;

import static java.util.stream.Collectors.toList;

public enum Versions {
    Version60 {
        @Override
        String dockerImage() {
            return "mongo:6.0.4-jammy";
        }

        @Override
        Version version() {
            return Version.forIntegers(6, 0, 4);
        }
    },
    Version50 {
        @Override
        String dockerImage() {
            return "5.0.15-focal";
        }

        @Override
        Version version() {
            return Version.forIntegers(5, 0, 15);
        }

    },
    Version44 {
        @Override
        String dockerImage() {
            return "4.4.19-focal";
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
            return "4.0.28-xenial";
        }

        @Override
        Version version() {
            return Version.forIntegers(4, 0, 28);
        }
    };

    public static Version latest() {
        return values()[0].version();
    }

    public static List<Version> list() {
        return Arrays.stream(values())
                .map(it -> it.version())
                .collect(toList());
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
        return version().toString();
    }
}