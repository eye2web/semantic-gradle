package nl.eye2web.semantic.gradle.extension

import org.gradle.api.Project

open class ChangeLog(project: Project) {

    val name = project.objects.property(String::class.java)
    val replaceWithLinks = project.objects.listProperty(ReplaceWithLink::class.java)

    init {
        name.convention("changelog")
    }

}