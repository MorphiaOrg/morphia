---
title: "Getting Started"
weight: 1
description: "Get started with Morphia"
---

# Getting Started

The recommended way to get started using Morphia in your project is with a dependency management system such as Maven or Gradle.

## Maven

Add the following dependency to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>dev.morphia.morphia</groupId>
        <artifactId>morphia-core</artifactId>
        <version>3.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

If you want to use a `-SNAPSHOT` build you will need to add the appropriate repository information to your pom:

```xml
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## Gradle

Gradle users can use the following dependency declaration:

```groovy
dependencies {
    implementation 'dev.morphia.morphia:morphia-core:3.0.0-SNAPSHOT'
}
```

To use `-SNAPSHOT` builds, an additional repository will be needed:

```groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots'
    }
}
```

{{% notice note %}}
Morphia 2.0 requires Java 11 or greater. Morphia has been tested on MongoDB servers as old as 3.6.15 up through the most recent builds. Morphia will likely work on older server versions, but those remain untested and no guarantees are made.
{{% /notice %}}