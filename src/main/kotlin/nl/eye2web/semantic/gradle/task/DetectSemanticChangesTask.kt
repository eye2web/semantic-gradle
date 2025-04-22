package nl.eye2web.semantic.gradle.task

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.eye2web.semantic.gradle.SemanticGradlePlugin
import nl.eye2web.semantic.gradle.SemanticGradlePlugin.Companion.SEMANTIC_BUILD_SERVICE
import nl.eye2web.semantic.gradle.build.configuration.model.BuildConfiguration
import nl.eye2web.semantic.gradle.service.DetectSemanticCommits
import nl.eye2web.semantic.gradle.build.service.SemanticBuildService
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.io.File

abstract class DetectSemanticChanges : DefaultTask() {

    @get:ServiceReference(SEMANTIC_BUILD_SERVICE)
    abstract val buildService: Property<SemanticBuildService>

    @get:Input
    val releaseBranchNames = project.objects.listProperty(String::class.java)

    @get:Input
    val buildInfoFile = project.objects.property(File::class.java)

    @get:Input
    val projectName = project.objects.property(String::class.java)

    init {
        group = "semanticGit"
        projectName.convention(project.name)
    }

    @TaskAction
    fun action() {
        val objectMapper = jacksonObjectMapper()

        val buildConfigurationList =
            objectMapper.readValue(buildInfoFile.get(), object : TypeReference<List<BuildConfiguration>>() {})

        val changes = DetectSemanticCommits().getChangesSincePreviousRelease(
            buildService.get().getGitRootDirectory().toFile(),
            buildConfigurationList,
            releaseBranchNames.get(),
            projectName.get()
        )

        logger.lifecycle("=================================================================================")
        logger.lifecycle("[${changes.gitCommits.size}] new commit(s) since last release")

        changes.gitCommits.forEach {
            logger.lifecycle("Commit [${it.commitHash}] [${it.commitMessage}]")
        }
    }
}