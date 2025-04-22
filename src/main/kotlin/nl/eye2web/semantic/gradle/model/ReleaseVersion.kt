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
        return "$projectName-${toString()}"
    }

    fun incrementMajor(): ReleaseVersion = copy(major = major + 1, minor = 0, patch = 0)
    fun incrementMinor(): ReleaseVersion = copy(minor = minor + 1, patch = 0)
    fun incrementPatch(): ReleaseVersion = copy(patch = patch + 1)

    override fun toString(): String {
        var version = "v$major.$minor.$patch"

        if (preRelease.isNotEmpty()) {
            version += "-$preRelease"
        }

        if (build.isNotEmpty()) {
            version += "+$build"
        }

        return version
    }
}