package nl.eye2web.semantic.gradle.extension

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Nested
import kotlin.jvm.java

open class SemanticGradleExtension(project: Project) {
    val projectName = project.objects.property(String::class.java)
    val releaseBranches = project.objects.listProperty(String::class.java)

    val defaultConventionCategoryName = project.objects.property(String::class.java)
    val conventionCategories = project.objects.setProperty(ConventionCategory::class.java)

    @Nested
    val changeLog = ChangeLog(project)

    init {
        val projName = project.name
        projectName.convention(projName)
        releaseBranches.convention(listOf<String>("main", "master"))

        defaultConventionCategoryName.convention("Fixes")

        conventionCategories.convention(
            setOf(
                ConventionCategory(
                    Increment.MAJOR, "Breaking changes",
                    listOf("break", "breaking")
                ),
                ConventionCategory(
                    Increment.MINOR, "New features",
                    listOf("feat")
                ),
                ConventionCategory(
                    Increment.PATCH, "Fixes",
                    listOf("fix", "patch", "chore", "test", "refactor", "style")
                ),
                ConventionCategory(
                    Increment.NONE, "Other",
                    listOf("docs")
                ),
            )
        )

    }

    fun changeLog(action: Action<ChangeLog>) {
        action.execute(changeLog)
    }
}