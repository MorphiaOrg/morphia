# Build Tasks


## Releases

The release process for Morphia is largely automated.  Releases are done using the `release` task like so:

    ./gradlew release

This will handle all the details of updating build files with the version number for the release, committing and tagging the build, and 
bumping the version number to the next -SNAPSHOT value.  Currently this introduces changes and commits locally though it does not push 
them to github automatically.  This is so that the release process can automatically tag the release as it pushes artifacts to sonatype. 
 Once a release is done, the documentation site will need to be updated as discussed below.

## Documentation Website

As with the Java driver, there are two halves to the documentation site:  `landing` and `reference`.  The documentation is updated using 
the gradle task `pushDocs`.  Depending on the current branch, this does a couple of different things.  When on `master`, this will update
 the landing page with the current code in the working directory.  It will also push the reference docs for the `master` branch under the
  `-SNAPSHOT` section on the landing page.  When on a release branch (e.g., `1.3.x`), `pushDocs` will only push the reference pages to 
  github.
  
### `landing`
The landing task is largely unremarkable.  It also requires a bit of handholding around releases.  When a new release is done on any 
branch, a couple of files will need to be updated:

  1. `docs/landing/data/releases.toml` -- the table of releases will need to be updated to reflect the new release.  The `current` tag 
  might need to be updated depending on the nature of the release.  
  1. `gradle.properties` -- the `latest_release` property might need to be updated if the release is against the most recent branch (as 
  opposed to a patch release on an older release).  
    
### `reference`
The `reference` is simultaneously more interesting and less work.  When the `reference` task is run, it will automatically update a 
number of different files with the current version information.  This makes it so that cross-document references in the document maintain
 fidelity with the version being generated so we don't end up with version mismatches.  This is done using the the version information 
 tracked in the gradle build itself and should require no intervention at this point.
