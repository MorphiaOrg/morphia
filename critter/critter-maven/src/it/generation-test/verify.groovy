// Verify that the critter plugin generated EntityModel, PropertyModel, and accessor classes
// for the Hotel entity, and that synthetic __read* methods were woven into the entity class.
// Mirrors the three assertions in the former CritterGenerationTest.

def basePackagePath = "dev/morphia/critter/it/gen"
def morphiaPackagePath = "${basePackagePath}/__morphia/hotel"

File outputDir = new File(basedir, "target/generated-classes/critter")
assert outputDir.exists() : "Generated classes directory should exist: ${outputDir.absolutePath}"

// ── entityModelShouldBeGenerated ──────────────────────────────────────────────

File entityModel = new File(outputDir, "${morphiaPackagePath}/HotelEntityModel.class")
assert entityModel.exists() : "HotelEntityModel.class should be generated at ${entityModel.absolutePath}"
println "OK: HotelEntityModel.class"

// ── propertyModelsShouldBeGenerated ──────────────────────────────────────────

def properties = ["Id", "Name", "Stars", "Tags"]
for (prop in properties) {
    File modelFile = new File(outputDir, "${morphiaPackagePath}/${prop}Model.class")
    assert modelFile.exists() : "${prop}Model.class should be generated at ${modelFile.absolutePath}"
    println "OK: ${prop}Model.class"
}

// ── syntheticAccessorMethodsShouldExist ──────────────────────────────────────
// Load the modified Hotel.class from the output directory via a URLClassLoader
// so we can use reflection to check for __read* methods, exactly as the original
// CritterGenerationTest did.  morphia-core (and its bson transitive dep) are
// available on the parent classloader because critter-maven depends on them.

def urls = [
    new File(basedir, "target/generated-classes/critter").toURI().toURL()
] as URL[]
def loader = new URLClassLoader(urls, Thread.currentThread().contextClassLoader)
def hotelClass = loader.loadClass("dev.morphia.critter.it.gen.Hotel")
def methodNames = hotelClass.getDeclaredMethods().collect { it.name }

for (prop in properties) {
    String readMethod = "__read${prop}"
    assert methodNames.contains(readMethod) :
        "Hotel should have synthetic method ${readMethod}. Found: ${methodNames}"
    println "OK: synthetic method ${readMethod}"

    String writeMethod = "__write${prop}"
    assert methodNames.contains(writeMethod) :
        "Hotel should have synthetic method ${writeMethod}. Found: ${methodNames}"
    println "OK: synthetic method ${writeMethod}"
}

println "All generation checks passed."
return true
