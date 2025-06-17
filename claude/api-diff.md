* remove the directory target/version-analysis
* for these steps, do all work in target/version-analysis including any temporary files created as part of your analysis
* any operations performed on files and directories underneath the working directory can proceed without explicit permission
* run the commands:
    * mvn dependency:get -Dartifact="dev.morphia.morphia:morphia-core:2.5.1-SNAPSHOT:jar:source" -DoutputDirectory=target/version-analysis
    * mvn dependency:copy -Dartifact="dev.morphia.morphia:morphia-core:2.5.1-SNAPSHOT:jar:source" -DoutputDirectory=target/version-analysis
* Read the source in the downloaded jar in target/version-analysis and read the sources available in core/src/main/java.
* compare the two versions.
* Looking at the types Aggregation, Datastore, and Query:
  * sort the methods by name and parameter count.
  * if a method is present in both versions, omit it.
  * list them side by side in an asciidoc table with gaps where a method is missing from the other version.
  * provide a summary as introduction and key observations at the end
  * generate this as an asciidoc table in docs/morphia-2.5-3.0-table.adoc.