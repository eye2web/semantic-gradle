package nl.eye2web.semantic.gradle.model

enum class CommitType(val title: String, val prefixTypes: List<String>) {
    BREAKING(
        "Breaking changes",
        listOf("break")
    ),
    FEATURE(
        "New features",
        listOf("feat")
    ),
    FIX(
        "Fixes",
        listOf(
            "fix",
            "hotfix"
        )
    ),
    OTHER(
        "Other changes",
        listOf(
            "chore",
            "docs",
            "refactor",
            "style",
            "test"
        )
    );

    companion object {
        fun fromPrefix(message: String): CommitType {
            return values().find { it.prefixTypes.any { type -> message.startsWith(type, true) } } ?: OTHER
        }
    }

}