package nl.eye2web.semantic.gradle

import nl.eye2web.semantic.gradle.build.configuration.SemanticGradleBuildConfigurationPlugin
import nl.eye2web.semantic.gradle.build.service.GitDirectoryBuildService
import nl.eye2web.semantic.gradle.task.DetectSemanticChanges
import nl.eye2web.semantic.gradle.build.configuration.task.IncludedBuildInfoTask
import org.gradle.api.Plugin
import org.gradle.api.Project

open class SemanticGradlePlugin : Plugin<Project> {

    companion object {
        const val SEMANTIC_RELEASE_ROOT_TASK = "detectSemanticChanges"

        const val GIT_DIRECTORY_BUILD_SERVICE = "gitDirectoryBuildService"
    }

    override fun apply(project: Project) {
        project.plugins.apply(SemanticGradleBuildConfigurationPlugin::class.java)

        project.gradle.sharedServices.registerIfAbsent(
            GIT_DIRECTORY_BUILD_SERVICE,
            GitDirectoryBuildService::class.java
        ) {
            parameters.getProjectPath().set(project.rootProject.projectDir)
        }

        val buildInfoTask = project.tasks.withType(IncludedBuildInfoTask::class.java).first()

        project.tasks.register(SEMANTIC_RELEASE_ROOT_TASK, DetectSemanticChanges::class.java) {
            dependsOn(buildInfoTask)
            buildInfoFile.convention(buildInfoTask.outputFile.asFile)

            releaseBranchNames.set(listOf("master"))
        }
    }
}