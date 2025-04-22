package nl.eye2web.semantic.gradle.build.service

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Objects

abstract class SemanticBuildService : BuildService<SemanticBuildService.Parameters> {

    interface Parameters : BuildServiceParameters {
        fun getProjectPath(): Property<File>
    }

    private val rootDirectory: Path = getGitRootDirectoryRecursively(parameters.getProjectPath().get().toPath())

    fun getGitRootDirectory(): Path {
        return rootDirectory
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