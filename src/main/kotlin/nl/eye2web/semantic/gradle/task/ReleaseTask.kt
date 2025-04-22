package nl.eye2web.semantic.gradle.task

import nl.eye2web.semantic.gradle.SemanticGradlePlugin.Companion.SEMANTIC_BUILD_SERVICE
import nl.eye2web.semantic.gradle.build.service.SemanticBuildService
import nl.eye2web.semantic.gradle.service.GitService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import kotlin.use

abstract class ReleaseTask() : DefaultTask() {

    @get:ServiceReference(SEMANTIC_BUILD_SERVICE)
    abstract val buildService: Property<SemanticBuildService>

    @get:Input
    val projectName = project.objects.property(String::class.java)

    init {
        group = "semanticGit"
        projectName.convention(project.name)
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun action() {

        // TODO check what kind of branch it is. Is release branch?

        val gitService = GitService()
        val git = gitService.openGit(buildService.get().getGitRootDirectory().toFile())
        val repository = gitService.openGitRepo(buildService.get().getGitRootDirectory().toFile())

        buildService.get().getDetectedChanges()?.let { semanticChanges ->
            semanticChanges.gitCommits
                .takeIf { it.isNotEmpty() }?.let {
                    createReleaseTag(repository, gitService, git)
                } ?: logger.warn("No commits found. No new release will be created.")
        } ?: logger.error("No semantic changes stored in build service! No release tag will be created.")
    }

    private fun createReleaseTag(
        repository: Repository,
        gitService: GitService,
        git: Git
    ) {
        RevWalk(repository).use { revWalk ->
            val commit = revWalk.parseAny(repository.findRef(repository.branch).objectId)

            val nextVersion = buildService.get().getDetectedChanges()!!.nextVersion()

            gitService.createTag(git, nextVersion.getVersionAsTag(), "Tag for project ${projectName.get()}", commit)
        }
    }
}