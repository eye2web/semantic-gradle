package nl.eye2web.semantic.gradle.task

import nl.eye2web.semantic.gradle.SemanticGradlePlugin.Companion.SEMANTIC_BUILD_SERVICE
import nl.eye2web.semantic.gradle.build.service.SemanticBuildService
import nl.eye2web.semantic.gradle.service.DetectSemanticCommits.Companion.getVersionRegex
import nl.eye2web.semantic.gradle.service.GitService
import org.eclipse.jgit.revwalk.RevWalk
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class CreateTagTask : DefaultTask() {

    @get:ServiceReference(SEMANTIC_BUILD_SERVICE)
    abstract val buildService: Property<SemanticBuildService>

    @get:Input
    val version = project.objects.property(String::class.java)

    @get:Input
    val projectName = project.objects.property(String::class.java)

    init {
        version.convention(project.providers.gradleProperty("version").orElse(""))

        group = "semanticGit"
        projectName.convention(project.name)
    }

    @TaskAction
    fun action() {
        if (!getVersionRegex().matches(version.get())) {
            throw GradleException(
                """Version [${version.get()}] has incorrect format or flag -Pversion={VERSION} is not set.
                | Examples :
                | -Pversion=0.0.0
                | -Pversion=0.0.0-RC1
                | -Pversion=0.0.0-RC1+BUILD123
            """.trimMargin()
            )
        }

        val gitService = GitService()
        val git = gitService.openGit(buildService.get().getGitRootDirectory().toFile())
        val repository = gitService.openGitRepo(buildService.get().getGitRootDirectory().toFile())

        RevWalk(repository).use { revWalk ->
            val commit = revWalk.parseAny(repository.findRef(repository.branch).objectId)

            val tag = "${projectName.get()}-v${version.get()}"
            gitService.createTag(git, tag, "Tag for project ${projectName.get()}", commit)
        }
    }
}