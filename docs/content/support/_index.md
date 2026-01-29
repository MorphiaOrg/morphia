---
title: "Support"
description: "Get help, report issues, and contribute to Morphia"
---

# Issues & Help

We are lucky to have a vibrant MongoDB Java community with lots of varying experience of using Morphia. We often find the quickest way to get support for general questions is through [GitHub discussions](https://github.com/MorphiaOrg/morphia/discussions), [the MongoDB community forums](https://community.mongodb.com/c/drivers-odms-connectors/), or through [Stack Overflow](https://stackoverflow.com/questions/tagged/morphia).

There is also a small but growing community on Discord which can be found [here](https://discord.gg/saZsJescBa).

## Bugs / Feature Requests

If you think you've found a bug or want to see a new feature in Morphia, please open an issue on [GitHub](https://github.com/MorphiaOrg/morphia/issues). Please provide as much information as possible (including version numbers) about the issue type and how to reproduce it. Ideally, if you can create a reproducer for the issue at hand, that would be even more helpful. To help with this, please take a look at the [reproducer](https://github.com/MorphiaOrg/reproducer) project. This will help you set up a quick environment for reproducing your issue and providing a working example to examine. This project can either be shared via a GitHub repo on your account or perhaps attaching a zip of the project to the associated issue.

{{% notice tip %}}
Providing a [reproducer](https://github.com/MorphiaOrg/reproducer) is the fastest way to help resolve your issue. It cuts down the guesswork and labor required to recreate the problem locally so that root causes can be investigated. If the repository is shared via GitHub (or other public repository), it even allows the opportunity for tweaks and suggestions to be made by both you and the maintainers to more quickly isolate and provide a fix.
{{% /notice %}}

## Pull Requests

We are happy to accept contributions to help improve Morphia. We will guide user contributions to ensure they meet the standards of the codebase. Please ensure that any pull requests include documentation, tests, and also pass the build checks.

To get started, check out the source and work on a branch:

```bash
$ git clone https://github.com/MorphiaOrg/morphia.git
$ cd morphia
$ git checkout -b myNewFeature
```

Finally, ensure that the code passes all the checks:

```bash
$ cd core
$ mvn -Dcode-audits
```
