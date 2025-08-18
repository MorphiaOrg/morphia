for the named class, create a copy of it and place in the kotlin subpackage under the class's package.  make the following changes:
 * The name of the new class and file should be the same as the java class but with Kotlin prepended to the name
 * The class should extend KotlinRewriteTest instead
 * each text block in the java test should be treated as a java program.  convert that text block source to kotlin in the new class.
 * every call to java() should be changed to a call to kotlin() and the //language comments updated to say kotlin instead. 