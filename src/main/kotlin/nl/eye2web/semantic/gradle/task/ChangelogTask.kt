package nl.eye2web.semantic.gradle.task

import nl.eye2web.semantic.gradle.SemanticGradlePlugin.Companion.ROOT_EXTENSION_NAME
import nl.eye2web.semantic.gradle.SemanticGradlePlugin.Companion.SEMANTIC_BUILD_SERVICE
import nl.eye2web.semantic.gradle.build.service.SemanticBuildService
import nl.eye2web.semantic.gradle.extension.ReplaceWithLink
import nl.eye2web.semantic.gradle.extension.SemanticGradleExtension
import nl.eye2web.semantic.gradle.model.GitCommit
import nl.eye2web.semantic.gradle.model.SemanticChanges
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import java.io.File

abstract class ChangelogTask() : DefaultTask() {

    @get:ServiceReference(SEMANTIC_BUILD_SERVICE)
    abstract val buildService: Property<SemanticBuildService>

    @get:OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    private val semanticVersioningExt: SemanticGradleExtension =
        project.extensions[ROOT_EXTENSION_NAME] as SemanticGradleExtension

    init {
        val projectDir = project.layout.projectDirectory
        val fileName = semanticVersioningExt.changeLog.fileName.map { "$it.md" }

        group = "semanticGit"
        outputFile.convention(projectDir.file(fileName))

        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun action() {

        val semanticChanges = buildService.get().getDetectedChanges()!!

        if (changelogContainsRelease(semanticChanges)) {
            logger.warn("Changelog already contains release for ${semanticChanges.nextVersion()}. Skipping changelog update.")
        } else {
            outputFile.get().asFile.prependText(createChangelogNotes())
        }
    }

    private fun changelogContainsRelease(semanticChanges: SemanticChanges): Boolean =
        outputFile.get().asFile.exists() && outputFile.get().asFile.readText()
            .contains(createMarkdownReleaseHeader(semanticChanges))

    private fun createChangelogNotes(): String {
        val semanticChanges = buildService.get().getDetectedChanges()!!

        val markDownChanges = semanticChanges.gitCommits.groupBy { it.type }.map { (type, commits) ->
            """
                
            #### ${type.title}
            %s
            """.trimIndent().trimMargin()
                .format(commits.map { commit ->
                    "- ${formatCommitMessage(commit)}"
                }.reduce { acc, s -> "$acc\n$s" })
        }.reduce { acc, s -> "$acc\n$s" }

        return "${createMarkdownReleaseHeader(semanticChanges)}\n$markDownChanges\n---\n"
    }

    private fun File.prependText(text: String) {
        val originalContent = if (this.exists()) this.readText() else ""
        this.writeText(text + originalContent)
    }

    private fun formatCommitMessage(commit: GitCommit): String {
        var commitMessage = commit.commitMessage

        if (semanticVersioningExt.changeLog.shouldLogShortCommitHash.get()) {
            commitMessage = appendShortCommitHash(commitMessage, commit)
        }

        var stripedCommitMessage = stripSemanticPostfix(commitMessage)

        semanticVersioningExt.changeLog.replaceWithLinks.get().forEach { replaceWithLink ->
            stripedCommitMessage = findAndReplaceWithLink(replaceWithLink, stripedCommitMessage)
        }

        return stripedCommitMessage
    }

    private fun findAndReplaceWithLink(
        replaceWithLink: ReplaceWithLink,
        commitMessage: String
    ): String {
        var commitMessage1 = commitMessage
        replaceWithLink.regex.matchEntire(commitMessage1)?.let { matchResult ->
            var replaceWith = replaceWithLink.replaceWith
            matchResult.groups.forEachIndexed { index, group ->
                group?.let {
                    replaceWith.indexOf("{$index}")
                        .takeIf { it >= 0 }
                        ?.let {
                            val url = replaceWith.replace("{$index}", group.value)
                            val markdownLink = "[${group.value}]($url)"
                            commitMessage1 = commitMessage1.replace(group.value, markdownLink)
                        }
                }
            }
        }
        return commitMessage1
    }

    private fun appendShortCommitHash(commitMessage: String, commit: GitCommit): String {
        return "$commitMessage - [${commit.shortCommitHash()}]"
    }

    private fun stripSemanticPostfix(commitMessage: String): String {
        val searchFor = ": "
        val startingIndex = commitMessage.indexOf(searchFor).let { if (it < 0) 0 else it + searchFor.length }
        return commitMessage.substring(startingIndex).trim()
    }

    private fun createMarkdownReleaseHeader(semanticChanges: SemanticChanges): String {
        return "### Release ${semanticChanges.nextVersion()}"
    }
}