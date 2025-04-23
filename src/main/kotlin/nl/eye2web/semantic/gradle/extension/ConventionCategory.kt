package nl.eye2web.semantic.gradle.extension

data class ConventionCategory(val increment: Increment, val name: String, val commitConventions: List<String>) {
    fun getConventions(): List<String> = commitConventions.map { "$it:" }
}