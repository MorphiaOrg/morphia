// Verify that the critter plugin generated EntityModel, PropertyModel, and accessor classes
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

// KotlinEntity property models: Id, Name, Count, Description
def kotlinEntityProperties = ["Id", "Name", "Count", "Description"]
for (prop in kotlinEntityProperties) {
    File modelFile = new File(outputDir, "${kotlinEntityMorphiaPath}/${prop}Model.class")
    assert modelFile.exists() : "${prop}Model.class should be generated for KotlinEntity at ${modelFile.absolutePath}"
    println "OK: KotlinEntity ${prop}Model.class"
}

// ── CompanionEntity EntityModel ───────────────────────────────────────────────

File companionEntityModel = new File(outputDir, "${companionEntityMorphiaPath}/CompanionEntityEntityModel.class")
assert companionEntityModel.exists() : "CompanionEntityEntityModel.class should be generated at ${companionEntityModel.absolutePath}"
println "OK: CompanionEntityEntityModel.class"

// CompanionEntity property models: only Id (companion object constant is not a mappable field)
File companionIdModel = new File(outputDir, "${companionEntityMorphiaPath}/IdModel.class")
assert companionIdModel.exists() : "IdModel.class should be generated for CompanionEntity at ${companionIdModel.absolutePath}"
println "OK: CompanionEntity IdModel.class"

// Assert companion object constant is NOT treated as a mappable field
File companionEntityNameModel = new File(outputDir, "${companionEntityMorphiaPath}/EntityNameModel.class")
assert !companionEntityNameModel.exists() : "ENTITY_NAME constant from companion object should NOT generate EntityNameModel.class"
println "OK: CompanionEntity companion constant is not treated as a mapped field"

// ── PropertyEntity EntityModel ────────────────────────────────────────────────

File propertyEntityModel = new File(outputDir, "${propertyEntityMorphiaPath}/PropertyEntityEntityModel.class")
assert propertyEntityModel.exists() : "PropertyEntityEntityModel.class should be generated at ${propertyEntityModel.absolutePath}"
println "OK: PropertyEntityEntityModel.class"

// PropertyEntity property models: Id, FirstName, LastName
def propertyEntityFields = ["Id", "FirstName", "LastName"]
for (prop in propertyEntityFields) {
    File modelFile = new File(outputDir, "${propertyEntityMorphiaPath}/${prop}Model.class")
    assert modelFile.exists() : "${prop}Model.class should be generated for PropertyEntity at ${modelFile.absolutePath}"
    println "OK: PropertyEntity ${prop}Model.class"
}

// Assert computed property 'fullName' does NOT generate an accessor (no backing field)
File fullNameModel = new File(outputDir, "${propertyEntityMorphiaPath}/FullNameModel.class")
assert !fullNameModel.exists() : "Computed property 'fullName' should NOT generate FullNameModel.class (no backing field)"
println "OK: PropertyEntity computed property 'fullName' did not generate an accessor"

println "All Kotlin entity generation checks passed."
return true
