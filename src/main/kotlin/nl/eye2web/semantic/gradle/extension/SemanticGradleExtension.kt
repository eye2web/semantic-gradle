package nl.eye2web.semantic.gradle.extension

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Nested
import kotlin.jvm.java

open class SemanticGradleExtension(project: Project) {
    val projectName = project.objects.property(String::class.java)
    val releaseBranches = project.objects.listProperty(String::class.java)

    @Nested
    val changeLog = ChangeLog(project)

    init {
        val projName = project.name
        projectName.convention(projName)
        releaseBranches.convention(listOf<String>("main", "master"))
    }

    fun changeLog(action: Action<ChangeLog>) {
        action.execute(changeLog)
    }
}