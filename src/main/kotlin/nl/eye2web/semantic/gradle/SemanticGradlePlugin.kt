package nl.eye2web.semantic.gradle

import nl.eye2web.semantic.gradle.build.configuration.SemanticGradleBuildConfigurationPlugin
import nl.eye2web.semantic.gradle.build.service.SemanticBuildService
import nl.eye2web.semantic.gradle.task.DetectSemanticChanges
import nl.eye2web.semantic.gradle.build.configuration.task.IncludedBuildInfoTask
import nl.eye2web.semantic.gradle.extension.SemanticGradleExtension
import nl.eye2web.semantic.gradle.task.ChangelogTask
import nl.eye2web.semantic.gradle.task.CreateTagTask
import org.gradle.api.Plugin
import org.gradle.api.Project

open class SemanticGradlePlugin : Plugin<Project> {

    companion object {

        const val CREATE_TAG_TASK = "createTag"

        const val SEMANTIC_RELEASE_ROOT_TASK = "detectSemanticChanges"

        const val CHANGELOG_TASK = "generateChangelog"

        const val SEMANTIC_BUILD_SERVICE = "semanticBuildService"

        const val ROOT_EXTENSION_NAME = "sematicVersioning"
    }

    override fun apply(project: Project) {
        project.plugins.apply(SemanticGradleBuildConfigurationPlugin::class.java)
        val semanticVersioningExt =
            project.extensions.create(ROOT_EXTENSION_NAME, SemanticGradleExtension::class.java, project)

        project.gradle.sharedServices.registerIfAbsent(
            SEMANTIC_BUILD_SERVICE,
            SemanticBuildService::class.java
        ) {
            parameters.getProjectPath().set(project.rootProject.projectDir)
        }

        val buildInfoTask = project.tasks.withType(IncludedBuildInfoTask::class.java).first()

        val detectSemanticChangesTask =
            project.tasks.register(SEMANTIC_RELEASE_ROOT_TASK, DetectSemanticChanges::class.java) {
                dependsOn(buildInfoTask)
                buildInfoFile.convention(buildInfoTask.outputFile.asFile)
                releaseBranchNames.set(semanticVersioningExt.releaseBranches)
            }

        project.tasks.register(CREATE_TAG_TASK, CreateTagTask::class.java)

        project.tasks.register(CHANGELOG_TASK, ChangelogTask::class.java) {
            dependsOn(detectSemanticChangesTask)
        }

    }
}