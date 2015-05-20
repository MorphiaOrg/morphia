+++
date = "2015-03-18T16:56:14Z"
title = "Issues & Help"
[menu.main]
  weight = 100
  pre = "<i class='fa fa-life-ring'></i>"
+++

# Issues & Help

We are lucky to have a vibrant MongoDB Java community with lots of varying
experience of using Morphia.  We often find the quickest way to get support for
general questions is through the [Morphia google group](https://groups.google.com/forum/#!forum/morphia),
[mongodb-user google group](http://groups.google.com/group/mongodb-user),
or through [stackoverflow](https://stackoverflow.com/questions/tagged/morphia).  Please also
refer to our own [support channels](http://www.mongodb.org/about/support) documentation.  If you have a question or think you've 
encountered a bug, the mailing list is the place to start.

## Bugs / Feature Requests

If you think you’ve found a bug or want to see a new feature in the Morphia, please open an issue on
 [github](https://github.com/mongodb/morphia/issues). Please provide as much information as possible (including version numbers) about the 
 issue type and how to reproduce it.

If you’ve identified a security vulnerability in a driver or any other
MongoDB project, please report it according to the [instructions here]({{< docsref "tutorial/create-a-vulnerability-report" >}}).

## Pull Requests

We are happy to accept contributions to help improve Morphia.  We will guide user contributions to ensure they meet the standards of the 
codebase. Please ensure that any pull requests include documentation, tests and also pass a the gradle checks.

To get started check out the source and work on a branch:

```bash
$ git clone https://github.com/mongodb/morphia.git
$ cd morphia
$ git checkout -b myNewFeature
```

Finally, ensure that the code passes gradle checks.
```bash
$ ./gradlew check
```
