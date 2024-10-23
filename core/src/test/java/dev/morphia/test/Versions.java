package dev.morphia.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import com.github.zafarkhaja.semver.Version;

import dev.morphia.sofia.Sofia;

import org.testcontainers.utility.DockerImageName;

import static com.github.zafarkhaja.semver.Version.forIntegers;

public enum Versions {
    Version8 {
        @Override
        Version version() {
            return forIntegers(8).setBuildMetadata("latest");
        }
    },
    Version7 {
        @Override
        Version version() {
            return forIntegers(7).setBuildMetadata("latest");
        }
    },
    Version6 {
        @Override
        Version version() {
            return forIntegers(6).setBuildMetadata("latest");
        }
    },
    Version5 {
        @Override
        Version version() {
            return forIntegers(5).setBuildMetadata("latest");
        }

    },
    Version44 {
        @Override
        Version version() {
            return forIntegers(4, 4, 19);
        }
    },
    Version42 {
        @Override
        Version version() {
            return forIntegers(4, 2, 24);
        }
    },
    Version40 {
        @Override
        Version version() {
            return forIntegers(4, 0, 28);
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

    public String withMinorVersion() {
        String minor = String.valueOf(version().getMajorVersion());
        if (version().getMinorVersion() != -1) {
            minor += "." + version().getMinorVersion();
        }

        return minor;
    }

    private boolean matches(Version target) {
        boolean latest = version().getBuildMetadata().equals("latest");
        return target.getMajorVersion() == version().getMajorVersion()
                && (latest || target.getMinorVersion() == version().getMinorVersion());
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
        Version suggested = Version.valueOf(joiner.toString());

        return Arrays.stream(values())
                .filter(it -> it.version().getMajorVersion() == suggested.getMajorVersion()
                        && it.version().getMinorVersion() == suggested.getMinorVersion())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(Sofia.unknownMongoDbVersion(suggested)));
    }

    final DockerImageName dockerImage() {
        Version version = version();
        String tag;
        if ("latest".equals(version.getBuildMetadata())) {
            tag = String.valueOf(version.getMajorVersion());
        } else {
            tag = version.toString();
        }
        return DockerImageName.parse("mongo:" + tag)
                .asCompatibleSubstituteFor("mongo");
    }

    abstract Version version();

    @Override
    public String toString() {
        Version version = version();

        var numbers = new StringJoiner(".");
        numbers.add(String.valueOf(version.getMajorVersion()));
        if (version.getMinorVersion() != 0 || version.getPatchVersion() != 0) {
            numbers.add(String.valueOf(version().getMinorVersion()))
                    .add(String.valueOf(version().getPatchVersion()));
        }
        return numbers.toString();
    }
}
