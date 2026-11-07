// Verify that the critter plugin generated EntityModel and accessor classes
// for the Person entity, and that synthetic __read* methods were woven into the entity class.

def basePackagePath = "com/example"
def morphiaPackagePath = "${basePackagePath}/__morphia/person"

File outputDir = new File(basedir, "target/generated-classes/critter")
assert outputDir.exists() : "Generated classes directory should exist: ${outputDir.absolutePath}"

// ── entityModelShouldBeGenerated ─────────────────────────────────────────────

File entityModel = new File(outputDir, "${morphiaPackagePath}/PersonEntityModel.class")
assert entityModel.exists() : "PersonEntityModel.class should be generated at ${entityModel.absolutePath}"
println "OK: PersonEntityModel.class"

// ── propertyAccessorsShouldBeGenerated ─────────────────────────────────────────

def properties = ["Id", "Name", "Age"]
for (prop in properties) {
    File accessorFile = new File(outputDir, "${morphiaPackagePath}/${prop}Accessor.class")
    assert accessorFile.exists() : "${prop}Accessor.class should be generated at ${accessorFile.absolutePath}"
    println "OK: ${prop}Accessor.class"
}

// ── syntheticAccessorMethodsShouldExist ─────────────────────────────────────
// Use javap to read method signatures without needing transitive dependencies.

File personClassFile = new File(outputDir, "com/example/Person.class")
assert personClassFile.exists() : "Person.class should exist in generated-classes at ${personClassFile.absolutePath}"

def proc = ["javap", "-p", personClassFile.absolutePath].execute()
def javapOutput = proc.text
def methodNames = javapOutput.readLines().collect { line ->
    def matcher = line =~ /\s+\S.*\s+(\w+)\s*\(/
    matcher ? matcher[0][1] : null
}.findAll { it != null }

for (prop in properties) {
    String readMethod = "__read${prop}"
    assert methodNames.contains(readMethod) :
        "Person should have synthetic method ${readMethod}. Found: ${methodNames}"
    println "OK: synthetic method ${readMethod}"

    String writeMethod = "__write${prop}"
    assert methodNames.contains(writeMethod) :
        "Person should have synthetic method ${writeMethod}. Found: ${methodNames}"
    println "OK: synthetic method ${writeMethod}"
}

println "All generation checks passed."
return true
