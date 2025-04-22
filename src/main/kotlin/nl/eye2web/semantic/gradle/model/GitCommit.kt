package nl.eye2web.semantic.gradle.model

data class GitCommit(val type: CommitType, val commitHash: String, val commitMessage: String) {

    constructor(commitHash: String, commitMessage: String) : this(
        determineType(commitMessage),
        commitHash,
        commitMessage
    )

    fun shortCommitHash(): String = commitHash.substring(0, 8)

    companion object {
        private fun determineType(commitMessage: String): CommitType {
            return CommitType.fromPrefix(commitMessage)
        }
    }
}
