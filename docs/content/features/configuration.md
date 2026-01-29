---
title: "Configuration"
weight: 1
description: "Configure Morphia for your application"
---

# Configuration

Starting in Morphia 3.0, configuration will be driven via a configuration file rather than programmatic configuration. The file should live in your build's resources folder and be named `META-INF/morphia-config.properties`.

The configuration mechanism, based on [Microprofile Config](https://microprofile.io/microprofile-config/), allows for a number of variations which were awkward, at best, and usually downright impossible when using the traditional programmatic configuration.

## Manually loading configurations

In some cases, you might find the need for multiple configuration files. Such scenarios include varying test environments/configurations, multiple dataset configurations, or externally supplied configurations. In such cases, you can manually load those configurations using `MorphiaConfig.load()`.

## Dynamic configuration creation

For many, the ability to dynamically create `MorphiaConfig` instances is not just a nicety but a hard requirement. In this case, there are methods on `MorphiaConfig` to return a new version of the configuration with the updated value.