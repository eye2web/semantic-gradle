package nl.eye2web.semantic.gradle.model

data class SemanticChanges(val previousVersion: ReleaseVersion, val gitCommits: List<GitCommit>)
