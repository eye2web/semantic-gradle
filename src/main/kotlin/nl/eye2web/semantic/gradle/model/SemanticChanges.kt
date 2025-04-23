package nl.eye2web.semantic.gradle.model

import nl.eye2web.semantic.gradle.extension.ConventionCategory
import nl.eye2web.semantic.gradle.extension.Increment

data class SemanticChanges(val previousVersion: ReleaseVersion, val gitCommits: List<GitCommit>) {
    fun nextVersion(conventionCategory: Set<ConventionCategory>): ReleaseVersion {

        val relatedCategories = conventionCategory.filter { category ->
            category.getConventions().any { commitConvention ->
                gitCommits.any { commit ->
                    commit.commitMessage.trim().startsWith(commitConvention, true)
                }
            }
        }

        relatedCategories.find { it.increment == Increment.MAJOR }
            ?.let {
                return previousVersion.incrementMajor()
            }

        relatedCategories.find { it.increment == Increment.MINOR }
            ?.let {
                return previousVersion.incrementMajor()
            }

        relatedCategories.find { it.increment == Increment.PATCH }
            ?.let {
                return previousVersion.incrementMajor()
            }

        return previousVersion
    }
}
