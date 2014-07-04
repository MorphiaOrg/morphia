package org.mongodb.morphia.release

class ReleasePluginExtension {
    String releaseVersion
    String snapshotVersion
    String javadocDir

    def setReleaseVersion(String version) {
        int indexOfSnapshotTag = version.indexOf('-SNAPSHOT')
        if (indexOfSnapshotTag >= 0) {
            snapshotVersion = version
            releaseVersion = version[0..<indexOfSnapshotTag]
        } else {
            // otherwise I assume the release version has already been updated to trim this from the build file
            releaseVersion = version
            snapshotVersion = "${releaseVersion}-SNAPSHOT"
        }
    }
}
