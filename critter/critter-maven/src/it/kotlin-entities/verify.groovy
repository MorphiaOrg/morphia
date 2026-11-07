// Verify that the critter plugin generated EntityModel and accessor classes
// for Kotlin entities: KotlinEntity, CompanionEntity, and PropertyEntity.
// Also verifies that the computed property 'fullName' on PropertyEntity (no backing field)
// does NOT generate an accessor, and that the companion object constant is not treated as
// a mappable field.

def basePackagePath = "dev/morphia/critter/it/kotlin"
def kotlinEntityMorphiaPath = "${basePackagePath}/__morphia/kotlinentity"
def companionEntityMorphiaPath = "${basePackagePath}/__morphia/companionentity"
def propertyEntityMorphiaPath = "${basePackagePath}/__morphia/propertyentity"

File outputDir = new File(basedir, "target/generated-classes/critter")
assert outputDir.exists() : "Generated classes directory should exist: ${outputDir.absolutePath}"

// ── KotlinEntity EntityModel ──────────────────────────────────────────────────

File kotlinEntityModel = new File(outputDir, "${kotlinEntityMorphiaPath}/KotlinEntityEntityModel.class")
assert kotlinEntityModel.exists() : "KotlinEntityEntityModel.class should be generated at ${kotlinEntityModel.absolutePath}"
println "OK: KotlinEntityEntityModel.class"

// KotlinEntity property accessors: Id, Name, Count, Description
def kotlinEntityProperties = ["Id", "Name", "Count", "Description"]
for (prop in kotlinEntityProperties) {
    File accessorFile = new File(outputDir, "${kotlinEntityMorphiaPath}/${prop}Accessor.class")
    assert accessorFile.exists() : "${prop}Accessor.class should be generated for KotlinEntity at ${accessorFile.absolutePath}"
    println "OK: KotlinEntity ${prop}Accessor.class"
}

// ── CompanionEntity EntityModel ───────────────────────────────────────────────

File companionEntityModel = new File(outputDir, "${companionEntityMorphiaPath}/CompanionEntityEntityModel.class")
assert companionEntityModel.exists() : "CompanionEntityEntityModel.class should be generated at ${companionEntityModel.absolutePath}"
println "OK: CompanionEntityEntityModel.class"

// CompanionEntity property accessors: only Id (companion object constant is not a mappable field)
File companionIdAccessor = new File(outputDir, "${companionEntityMorphiaPath}/IdAccessor.class")
assert companionIdAccessor.exists() : "IdAccessor.class should be generated for CompanionEntity at ${companionIdAccessor.absolutePath}"
println "OK: CompanionEntity IdAccessor.class"

// Assert companion object constant is NOT treated as a mappable field
File companionEntityNameAccessor = new File(outputDir, "${companionEntityMorphiaPath}/EntityNameAccessor.class")
assert !companionEntityNameAccessor.exists() : "ENTITY_NAME constant from companion object should NOT generate EntityNameAccessor.class"
println "OK: CompanionEntity companion constant is not treated as a mapped field"

// ── PropertyEntity EntityModel ────────────────────────────────────────────────

File propertyEntityModel = new File(outputDir, "${propertyEntityMorphiaPath}/PropertyEntityEntityModel.class")
assert propertyEntityModel.exists() : "PropertyEntityEntityModel.class should be generated at ${propertyEntityModel.absolutePath}"
println "OK: PropertyEntityEntityModel.class"

// PropertyEntity property accessors: Id, FirstName, LastName
def propertyEntityFields = ["Id", "FirstName", "LastName"]
for (prop in propertyEntityFields) {
    File accessorFile = new File(outputDir, "${propertyEntityMorphiaPath}/${prop}Accessor.class")
    assert accessorFile.exists() : "${prop}Accessor.class should be generated for PropertyEntity at ${accessorFile.absolutePath}"
    println "OK: PropertyEntity ${prop}Accessor.class"
}

// Assert computed property 'fullName' does NOT generate an accessor (no backing field)
File fullNameAccessor = new File(outputDir, "${propertyEntityMorphiaPath}/FullNameAccessor.class")
assert !fullNameAccessor.exists() : "Computed property 'fullName' should NOT generate FullNameAccessor.class (no backing field)"
println "OK: PropertyEntity computed property 'fullName' did not generate an accessor"

println "All Kotlin entity generation checks passed."
return true
