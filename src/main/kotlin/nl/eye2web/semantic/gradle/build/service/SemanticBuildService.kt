package nl.eye2web.semantic.gradle.build.service

import nl.eye2web.semantic.gradle.model.SemanticChanges
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap

abstract class SemanticBuildService : BuildService<SemanticBuildService.Parameters> {

    interface Parameters : BuildServiceParameters {
        fun getProjectPath(): Property<File>
    }

    private val rootDirectory: Path = getGitRootDirectoryRecursively(parameters.getProjectPath().get().toPath())

    private val detectedChanges: ConcurrentHashMap<String, SemanticChanges> = ConcurrentHashMap()

    fun getGitRootDirectory(): Path {
        return rootDirectory
    }

    fun storeDetectedChanges(name: String, detectedChanges: SemanticChanges) {
        if (this.detectedChanges.contains(name)) {
            this.detectedChanges.replace(name, detectedChanges)
        } else {
            this.detectedChanges.put(name, detectedChanges)
        }
    }

    fun getDetectedChanges(name: String): SemanticChanges? {
        return detectedChanges.get(name) ?: throw Exception("No changes detected for project [$name]")
    }

    private fun getGitRootDirectoryRecursively(path: Path): Path {
        if (Files.exists(path.resolve(".git"))) {
            return path
        }

        val parent = path.parent;
        if (Objects.nonNull(parent))
            return getGitRootDirectoryRecursively(parent);

        throw Exception("No Git root directory detected!");
    }

}