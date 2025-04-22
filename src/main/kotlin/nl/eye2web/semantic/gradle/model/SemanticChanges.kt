package nl.eye2web.semantic.gradle.model

data class SemanticChanges(val previousVersion: ReleaseVersion, val gitCommits: List<GitCommit>) {
    fun nextVersion(): ReleaseVersion {
        
        gitCommits.find { it.type == CommitType.BREAKING }?.let {
            return previousVersion.incrementMajor()
        }

        gitCommits.find { it.type == CommitType.FEATURE }?.let {
            return previousVersion.incrementMinor()
        }

        gitCommits.find { it.type == CommitType.OTHER }?.let {
            return previousVersion.incrementPatch()
        }

        return previousVersion
    }
}
