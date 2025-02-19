package nl.eye2web.semantic.gradle.model

data class ReleaseVersion(
    val projectName: String,
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String,
    val build: String
) {
    fun getVersionAsTag(): String {
        var version = "$projectName-v$major.$minor.$patch"

        if (preRelease.isNotEmpty()) {
            version += "-$preRelease"
        }

        if (build.isNotEmpty()) {
            version += "+$build"
        }

        return version
    }
}