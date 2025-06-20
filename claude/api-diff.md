* for these steps, do all work in target/version-analysis including any temporary files created as part of your analysis
* any operations performed on files and directories underneath the working directory can proceed without explicit permission
* run the commands:
    * mvn dependency:get -Dartifact="dev.morphia.morphia:morphia-core:2.5.1-SNAPSHOT:jar:source" -DoutputDirectory=target/version-analysis
    * mvn dependency:copy -Dartifact="dev.morphia.morphia:morphia-core:2.5.1-SNAPSHOT:jar:source" -DoutputDirectory=target/version-analysis
* Read the source in the downloaded jar in target/version-analysis and read the sources available in core/src/main/java.
* compare the two versions
  * looking only at the following types
    * Aggregation
    * Datastore
    * Query
  * Consider only the interface types not any implementation classes 
  * sort the methods by name and parameter count.
  * list them side by side in an asciidoc table with gaps where a method is missing from the other version.
  * do not list a status column
  * create summary introduction and key observations about the changes at the end.
  * refer to the prior version only as major.minor version numbers 
  * output this report to docs/modules/ROOT/pages/morphia-2.5-3.0-table.adoc