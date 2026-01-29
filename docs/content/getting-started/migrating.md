---
title: "Migration"
weight: 3
description: "Migration guides for Morphia versions"
---

# Migration

## Migrating to 3.0

More detailed information will be made available once those details stabilize but at the moment there are two things to keep an eye on:

1. Upgrades will be as automatic as they can be made to be via OpenRewrite recipes that should take care of almost all of the work if things go as well as hoped.
2. The 3.0 API is getting some long overdue cleanups and optimizations. A brief summary can be found in the [API Changes](/getting-started/api-changes/) page. This page will evolve heavily as the release nears.

## Migrating to 2.5

Morphia 2.5 is primarily a driver upgrade. Changes in the driver are incompatible with the 2.4 driver and so Morphia 2.5 requires a driver version of 5.x or greater. If you need a 4.x driver for some reason, you can use Morphia 2.4.x. The 2.5.x series can only support 5.x and greater.

In preparation for 3.0, deprecations will be incrementally made in the 2.5.x line with replacements where feasible. Updating away from these deprecations will make the transition to 3.0 much smoother.

## Migrating to 2.4

### Configuration

Morphia 2.4 introduces file-based configuration instead of the programmatic configuration currently in use. The file should live in your build's resources folder and be named `META-INF/morphia-config.properties`. More information can be found in the [configuration section](/features/configuration/).

`MorphiaConfig` is the programmatic option that replaces the `MapperOptions` class. This class is a builder for the configuration and works the same way as the `MapperOptions.Builder` class. This class is immutable which allows for safe sharing between `Datastore` instances should you need to do so.