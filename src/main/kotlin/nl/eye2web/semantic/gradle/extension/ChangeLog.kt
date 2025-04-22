package nl.eye2web.semantic.gradle.extension

import org.gradle.api.Project

open class ChangeLog(project: Project) {

    val shouldLogShortCommitHash = project.objects.property(Boolean::class.java)

    val fileName = project.objects.property(String::class.java)
    val replaceWithLinks = project.objects.listProperty(ReplaceWithLink::class.java)

    init {
        fileName.convention("CHANGELOG")
        shouldLogShortCommitHash.convention(false)
    }

}