// Verify that the critter plugin generated EntityModel, PropertyModel, and accessor classes
// for the Person entity, and that synthetic __read* methods were woven into the entity class.

def basePackagePath = "com/example"
def morphiaPackagePath = "${basePackagePath}/__morphia/person"

File outputDir = new File(basedir, "target/generated-classes/critter")
assert outputDir.exists() : "Generated classes directory should exist: ${outputDir.absolutePath}"

// ── entityModelShouldBeGenerated ─────────────────────────────────────────────

File entityModel = new File(outputDir, "${morphiaPackagePath}/PersonEntityModel.class")
assert entityModel.exists() : "PersonEntityModel.class should be generated at ${entityModel.absolutePath}"
println "OK: PersonEntityModel.class"

// ── propertyModelsShouldBeGenerated ─────────────────────────────────────────

def properties = ["Id", "Name", "Age"]
for (prop in properties) {
    File modelFile = new File(outputDir, "${morphiaPackagePath}/${prop}Model.class")
    assert modelFile.exists() : "${prop}Model.class should be generated at ${modelFile.absolutePath}"
    println "OK: ${prop}Model.class"
}

// ── syntheticAccessorMethodsShouldExist ─────────────────────────────────────
// Load the modified Person.class from the output directory via a URLClassLoader
// so we can use reflection to check for __read* methods.

def urls = [
    new File(basedir, "target/generated-classes/critter").toURI().toURL()
] as URL[]
def loader = new URLClassLoader(urls, Thread.currentThread().contextClassLoader)
def personClass = loader.loadClass("com.example.Person")
def methodNames = personClass.getDeclaredMethods().collect { it.name }

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
