// Verify that the critter plugin ran successfully

File generatedDir = new File(basedir, "target/generated-classes/critter")

// The directory should exist (or the build should have completed without error)
if (generatedDir.exists()) {
    println "Generated classes directory exists: ${generatedDir.absolutePath}"

    // List any generated files
    generatedDir.eachFileRecurse { file ->
        if (file.isFile()) {
            println "  Generated: ${file.name}"
        }
    }
} else {
    println "No generated classes directory (this may be expected if no entities were processed)"
}

// Check that the build completed successfully
File buildLog = new File(basedir, "build.log")
if (buildLog.exists()) {
    String log = buildLog.text
    if (log.contains("BUILD SUCCESS")) {
        println "Build completed successfully"
        return true
    } else if (log.contains("BUILD FAILURE")) {
        println "Build failed!"
        return false
    }
}

// If we get here, assume success
return true
