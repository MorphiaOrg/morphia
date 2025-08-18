in the src/test/java/dev/morphia/rewrite/recipes/test/pipeline directory, perform the following tasks:

* Look at src/test/java/dev/morphia/rewrite/recipes/test/pipeline/PipelineRewriteTest.java and 
  src/test/java/dev/morphia/rewrite/recipes/test/pipeline/kotlin/KotlinPipelineRewriteTest.java for an example of the changes to make
* Do not modify this example files
* For each test in the given directory, create a copy of each test
  * prepend "Kotlin" to the class name
  * put the new class in a .kotlin subpackage
* The import for the "java()" method in the openrewrite package should be replaced with an import of the "kotlin()" method
* The text in each string block should be converted to kotlin
* then run the "Test Rewrites" run configuration in IDEA to test the new classes