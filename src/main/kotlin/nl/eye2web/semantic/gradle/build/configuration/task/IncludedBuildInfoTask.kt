package nl.eye2web.semantic.gradle.build.configuration.task

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.eye2web.semantic.gradle.SemanticGradlePlugin.Companion.SEMANTIC_BUILD_SERVICE
import nl.eye2web.semantic.gradle.build.configuration.extension.BuildConfigurationExtension
import nl.eye2web.semantic.gradle.build.configuration.model.BuildConfiguration
import nl.eye2web.semantic.gradle.build.service.SemanticBuildService

import org.gradle.api.DefaultTask

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class IncludedBuildInfoTask : DefaultTask() {

    @get:ServiceReference(SEMANTIC_BUILD_SERVICE)
    abstract val buildService: Property<SemanticBuildService>

    @get:Input
    val projectName = project.objects.property(String::class.java)

    @get:Input
    val projectPath = project.objects.property(File::class.java)

    @get:Input
    val otherScanPaths = project.objects.listProperty(File::class.java)

    @get:Input
    val excludeIncludedBuilds = project.objects.listProperty(String::class.java)

    @get:Input
    val includedBuildsBuildFile = project.objects.setProperty(File::class.java)

    @get:OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    init {
        val buildConfigurationExtension = project.extensions.getByType(BuildConfigurationExtension::class.java)

        group = "semanticGit"
        projectName.convention(buildConfigurationExtension.projectName)
        otherScanPaths.convention(buildConfigurationExtension.otherScanPaths)
        excludeIncludedBuilds.convention(buildConfigurationExtension.excludeIncludedBuilds)

        val outputFilePath = "release/includedBuildInfo.json"

        projectPath.convention(project.layout.projectDirectory.asFile)
        outputFile.convention(project.layout.buildDirectory.file(outputFilePath))
        includedBuildsBuildFile.convention(
            project.gradle.includedBuilds
                .filter {
                    !excludeIncludedBuilds.get().contains(it.name)
                }.map { it.projectDir.resolve("build/$outputFilePath") }
                .toSet())
    }

    @TaskAction
    fun action() {
        val objectMapper = jacksonObjectMapper()

        val gitPath = buildService.get().getGitRootDirectory().toFile()

        val relativePaths =
            otherScanPaths.get().toList().map { it.relativeUnixPath(gitPath) }.toMutableList()

        val relativeProjectPath = projectPath.get().relativeUnixPath(gitPath).substring(1)
        relativePaths.add(relativeProjectPath)

        val buildConfigurationList = mutableSetOf(BuildConfiguration(projectName.get(), relativePaths))

        includedBuildsBuildFile.get().filter {
            it.exists()
        }.forEach { buildFile ->
            buildConfigurationList.addAll(
                objectMapper.readValue(
                    buildFile,
                    object : TypeReference<List<BuildConfiguration>>() {})
            )
        }

        objectMapper.writeValue(outputFile.get().asFile, buildConfigurationList)
    }

    private fun File.relativeUnixPath(relativeTo: File): String {
        return this.getRelativePath(relativeTo).path.replace("\\", "/")
    }

    private fun File.getRelativePath(relativeTo: File): File {
        return File(this.absolutePath.substringAfter(relativeTo.path, this.invariantSeparatorsPath))
    }
}