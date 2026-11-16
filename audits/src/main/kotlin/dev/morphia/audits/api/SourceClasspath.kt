package dev.morphia.audits.api

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.jar.Attributes
import java.util.jar.JarFile

data class ClasspathInput(val index: Int, val name: String, val sha256: String)

internal object SourceClasspath {
    fun digest(path: Path, index: Int): ClasspathInput {
        require(Files.isDirectory(path) || Files.isRegularFile(path)) {
            "Missing classpath entry: $path"
        }
        if (Files.isRegularFile(path)) {
            JarFile(path.toFile()).use { jar ->
                require(
                    jar.manifest
                        ?.mainAttributes
                        ?.getValue(Attributes.Name.CLASS_PATH)
                        .isNullOrBlank()
                ) {
                    "Classpath archives must not declare a manifest Class-Path: $path"
                }
            }
        }
        val sha =
            if (Files.isRegularFile(path)) hash(path)
            else {
                val digest = MessageDigest.getInstance("SHA-256")
                val files =
                    Files.walk(path).use { entries ->
                        entries
                            .sorted()
                            .toList()
                            .onEach {
                                require(!Files.isSymbolicLink(it)) {
                                    "Classpath entries must not contain symlinks: $it"
                                }
                            }
                            .filter { Files.isRegularFile(it) }
                    }
                files.forEach { file ->
                    digest.update(
                        path.relativize(file).joinToString("/").toByteArray(Charsets.UTF_8)
                    )
                    digest.update(0.toByte())
                    digest.update(hash(file).toByteArray(Charsets.US_ASCII))
                    digest.update(10.toByte())
                }
                HexFormat.of().formatHex(digest.digest())
            }
        return ClasspathInput(index, path.fileName.toString(), sha)
    }

    fun hash(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return HexFormat.of().formatHex(digest.digest())
    }
}
