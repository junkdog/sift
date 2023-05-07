package sift.core.asm

import net.onedaybeard.collectionsby.filterBy
import sift.core.element.AsmClassNode
import java.io.InputStream
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

fun resolveClassNodes(source: String): List<AsmClassNode> {
    val isUri = Regex("^[a-z-]{2,}:") in source
    val mavenArtifact = MavenArtifact.parse(source)

    return when {
        mavenArtifact != null -> classNodesOf(mavenArtifact)
        isUri                 -> classNodesOf(URI(source))
        else                  -> classNodes(resolvePath(source))
    }
}

private fun resolvePath(source: String): Path {
    return Path.of(source, "target/classes").takeIf(Path::exists)
        ?: Path.of(source).takeIf(Path::exists)
        ?: error("path does not exist: $source")
}

@OptIn(ExperimentalPathApi::class)
private fun classNodesOf(uri: URI): List<AsmClassNode> {
    val f = Files.createTempFile("sift_download", ".jar")

    return try {
        uri.toURL()
            .openStream()
            .use(InputStream::readAllBytes)
            .let(f::writeBytes)

        FileSystems.newFileSystem(f).use { fs ->
            fs.rootDirectories
                .flatMap { root -> root.walk() }
                .filterBy(Path::extension, "class")
                .map(Path::readBytes)
                .map(::classNode)
        }
    } finally {
        f.deleteIfExists()
    }
}

private fun classNodesOf(artifact: MavenArtifact): List<AsmClassNode> {
    fun tryDownload(uri: URI): List<AsmClassNode>? {
        return try {
            classNodesOf(uri)
        } catch (e: Exception) {
            null
        }
    }

    return sequenceOf(artifact.mavenLocal, artifact.mavenCentral)
        .mapNotNull(::tryDownload)
        .firstOrNull()
        ?: error("failed downloading maven artifact: $artifact")
}

private data class MavenArtifact(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val extension: String = "jar",
) {
    private val repoPath: String
        get() = "${groupId.replace('.', '/')}/$artifactId/$version/$artifactId-$version.$extension"

    val mavenLocal: URI
        get() = URI("file://${System.getProperty("user.home")}/.m2/repository/${repoPath}")
    val mavenCentral: URI
        get() = URI("https://repo1.maven.org/maven2/${repoPath}")

    companion object {
        private val identifyMavenArtifact =
            Regex("""(?<groupId>[\w_.\-]+):(?<artifactId>[\w_.\-]+)(?::jar)?:(?<version>[\w_.\-]+)""")::matchEntire

        fun parse(input: String): MavenArtifact? {
            val match = identifyMavenArtifact(input) ?: return null
            return MavenArtifact(
                match.groups["groupId"]!!.value,
                match.groups["artifactId"]!!.value,
                match.groups["version"]!!.value,
            )
        }
    }
}
