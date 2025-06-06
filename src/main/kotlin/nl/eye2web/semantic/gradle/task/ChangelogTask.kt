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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

abstract class ChangelogTask() : DefaultTask() {

    @get:ServiceReference(SEMANTIC_BUILD_SERVICE)
    abstract val buildService: Property<SemanticBuildService>

    val isReleased = project.objects.property(Boolean::class.java)

    @get:OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    private val semanticVersioningExt: SemanticGradleExtension =
        project.extensions[ROOT_EXTENSION_NAME] as SemanticGradleExtension

    init {
        val projectDir = project.layout.projectDirectory
        val fileName = semanticVersioningExt.changeLog.fileName.map { "$it.md" }

        group = "semanticGit"

        isReleased.convention(false)

        outputFile.convention(projectDir.file(fileName))

        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun action() {

        val semanticChanges = buildService.get().getDetectedChanges(semanticVersioningExt.projectName.get())!!

        if (isReleased.get() && changelogContainsRelease(semanticChanges)) {
            logger.warn("Changelog already contains release for ${semanticChanges.nextVersion(semanticVersioningExt.conventionCategories.get())}. Skipping changelog update.")
        } else {
            val changelogNotes = createChangelogNotes()

            changelogNotes?.let {
                outputFile.get().asFile.prependOrReplaceText(it)
            } ?: logger.warn("No changes detected. Skipping changelog update.")
        }
    }

    private fun changelogContainsRelease(semanticChanges: SemanticChanges): Boolean =
        outputFile.get().asFile.exists() && outputFile.get().asFile.readText()
            .contains(createMarkdownReleaseHeader(semanticChanges))

    private fun createChangelogNotes(): String? {
        val semanticChanges = buildService.get().getDetectedChanges(semanticVersioningExt.projectName.get())!!

        if (semanticChanges.gitCommits.isEmpty()) {
            return null
        }

        val categoryGroupedCommitMessages = groupSemanticChangesByCategory(semanticChanges)

        return createMarkdownChangelog(categoryGroupedCommitMessages, semanticChanges)
    }

    private fun createMarkdownChangelog(
        categoryGroupedCommitMessages: List<Pair<String, MutableList<String>>>,
        semanticChanges: SemanticChanges
    ): String {
        val markDownChanges = categoryGroupedCommitMessages
            .filter { pair -> pair.second.isNotEmpty() }
            .map { (category, commitMessages) ->
                """
                    
                #### $category
                %s
                """.trimIndent().trimMargin()
                    .format(commitMessages.map { commitMessage ->
                        "- ${formatCommitMessageLinks(commitMessage)}"
                    }.fold("") { acc, s -> "$acc\n$s" }.trim())
            }.fold("") { acc, s -> "$acc\n$s" }.trim()

        return "${
            getChangelogNotesHeader(
                getVersion(semanticChanges)
            )
        }${createMarkdownReleaseHeader(semanticChanges)}\n$markDownChanges\n---\n${
            getChangelogNotesFooter(
                getVersion(semanticChanges)
            )
        }"
    }

    private fun groupSemanticChangesByCategory(semanticChanges: SemanticChanges): List<Pair<String, MutableList<String>>> {
        val gitCommits = semanticChanges.gitCommits.toMutableList()

        val categoryGroupedCommitMessages = semanticVersioningExt.conventionCategories.get().map { category ->
            Pair(
                category.name,
                gitCommits.filter { commit ->
                    category.getConventions().any { commitConvention ->
                        commit.commitMessage.trim().startsWith(commitConvention, true)
                    }
                }.map { commit ->
                    gitCommits.remove(commit)
                    var commitMessage = commit.commitMessage
                    category.getConventions().forEach { commitConvention ->
                        commitMessage = commitMessage.replace(commitConvention, "", true)
                    }

                    if (semanticVersioningExt.changeLog.shouldLogShortCommitHash.get()) {
                        commitMessage = appendShortCommitHash(commitMessage, commit)
                    }

                    commitMessage
                }.toMutableList()
            )
        }

        // Add all other commit messages to default category
        val defaultCategoryName = semanticVersioningExt.defaultConventionCategoryName
        categoryGroupedCommitMessages.forEach { (categoryName, commitMessages) ->
            if (categoryName == defaultCategoryName.get()) {
                commitMessages.addAll(gitCommits.map { commit ->
                    var commitMessage = commit.commitMessage.trim()
                    if (semanticVersioningExt.changeLog.shouldLogShortCommitHash.get()) {
                        commitMessage = appendShortCommitHash(commitMessage, commit)
                    }
                    commitMessage
                })
            }
        }
        return categoryGroupedCommitMessages
    }

    private fun getChangelogNotesHeader(version: String): String {
        return "<!-- <$version> -->\n"
    }

    private fun getChangelogNotesFooter(version: String): String {
        return "<!-- </$version> -->\n"
    }

    private fun getVersion(semanticChanges: SemanticChanges): String =
        if (isReleased.get()) semanticChanges.nextVersion(semanticVersioningExt.conventionCategories.get())
            .toString() else getUnreleasedVersion()

    private fun getUnreleasedVersion(): String =
        "Unreleased"

    private fun File.prependOrReplaceText(text: String) {

        val originalContentLines = if (this.exists()) this.readLines() else listOf()

        val header = getChangelogNotesHeader(getUnreleasedVersion())
        val footer = getChangelogNotesFooter(getUnreleasedVersion())

        if (originalContentLines.contains(header.trim()) && originalContentLines.contains(footer.trim())) {
            logger.lifecycle("Found Unreleased changes. Updating Unreleased changes")
            var skip = false

            originalContentLines.filter { line ->
                if (line.contains(header.trim())) {
                    skip = true
                } else if (line.contains(footer.trim())) {
                    skip = false
                    return@filter false
                }

                return@filter !skip
            }
                .fold("") { acc, s -> "$acc\n$s" }
                .let {
                    this.writeText(text + it.trim())
                }
        } else {
            this.writeText(text + originalContentLines.fold("") { acc, s -> "$acc\n$s" }.trim())
        }
    }

    private fun formatCommitMessageLinks(commit: String): String {
        var commitMessage = commit

        semanticVersioningExt.changeLog.replaceWithLinks.get().forEach { replaceWithLink ->
            commitMessage = findAndReplaceWithLink(replaceWithLink, commitMessage)
        }

        return commitMessage
    }

    private fun findAndReplaceWithLink(
        replaceWithLink: ReplaceWithLink,
        commitMessage: String
    ): String {
        var commitMessage1 = commitMessage
        replaceWithLink.regex.findAll(commitMessage1).forEach { matchResult ->
            val replaceWith = replaceWithLink.replaceWith
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

    private fun createMarkdownReleaseHeader(semanticChanges: SemanticChanges): String {
        val date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

        return "### [${semanticChanges.previousVersion} --> ${getVersion(semanticChanges)}] - $date"
    }
}