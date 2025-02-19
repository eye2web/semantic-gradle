package nl.eye2web.semantic.gradle.build.configuration.extension

import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.io.File

abstract class BuildConfigurationExtension(project: Project) {
    // The name of the project
    abstract val projectName: Property<String>

    // Other files and dirs to scan for git changes
    val otherScanPaths = project.objects.listProperty(File::class.java)

    // The name of includedBuilds to exclude
    val excludeIncludedBuilds = project.objects.listProperty(String::class.java)
}