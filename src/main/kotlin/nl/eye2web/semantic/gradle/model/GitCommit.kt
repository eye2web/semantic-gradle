package nl.eye2web.semantic.gradle.model

data class GitCommit(val commitHash: String, val commitMessage: String) {
    
    fun shortCommitHash(): String = commitHash.substring(0, 8)
}
