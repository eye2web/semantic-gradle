package nl.eye2web.semantic.gradle.build.configuration


import nl.eye2web.semantic.gradle.SemanticGradlePlugin
import nl.eye2web.semantic.gradle.build.configuration.extension.BuildConfigurationExtension
import nl.eye2web.semantic.gradle.build.service.SemanticBuildService
import nl.eye2web.semantic.gradle.build.configuration.task.IncludedBuildInfoTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskReference

class SemanticGradleBuildConfigurationPlugin : Plugin<Project> {

    companion object {
        const val BUILD_INFO_TASK_NAME = "semanticBuildConfiguration"
        const val SEMANTIC_GRADLE_BUILD_CONFIG_EXTENSION_NAME = "semanticBuild"
    }

    override fun apply(project: Project) {
        val buildconfigExt = createProjectExtension(project)

        project.gradle.sharedServices.registerIfAbsent(
            SemanticGradlePlugin.Companion.SEMANTIC_BUILD_SERVICE,
            SemanticBuildService::class.java
        ) {
            parameters.getProjectPath().set(project.rootProject.projectDir)
        }

        project.tasks.register(BUILD_INFO_TASK_NAME, IncludedBuildInfoTask::class.java) {
            dependsOn(includedBuildsInfoTasks(project, buildconfigExt))
        }
    }

    private fun includedBuildsInfoTasks(
        project: Project,
        buildconfigExt: BuildConfigurationExtension
    ): Provider<List<TaskReference>> {
        val gradle = project.gradle
        return project.provider {
            gradle.includedBuilds.filter { !buildconfigExt.excludeIncludedBuilds.get().contains(it.name) }
                .map { it.task(":$BUILD_INFO_TASK_NAME") }
        }
    }

    private fun createProjectExtension(project: Project): BuildConfigurationExtension {
        val semanticGradleExt =
            project.extensions.create(
                SEMANTIC_GRADLE_BUILD_CONFIG_EXTENSION_NAME,
                BuildConfigurationExtension::class.java
            )

        semanticGradleExt.projectName.convention(project.name)
        semanticGradleExt.otherScanPaths.convention(listOf())
        semanticGradleExt.excludeIncludedBuilds.convention(listOf())

        return semanticGradleExt
    }
}