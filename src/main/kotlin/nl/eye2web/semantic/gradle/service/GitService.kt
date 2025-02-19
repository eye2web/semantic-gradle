package nl.eye2web.semantic.gradle.service

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import java.util.stream.StreamSupport.*

class GitService {

    fun openGitRepo(girRepoDir: File): Repository {
        return openGit(girRepoDir).repository
    }

    fun openGit(girRepoDir: File): Git {
        return Git.open(girRepoDir)
    }

    fun findLatestTagBy(repository: Repository, releaseBranchRef: Ref, regex: Regex): Ref? {
        val tags = repository.getAllTags()

        RevWalk(repository).use { revWalk ->

            val sortedTags = revWalk.sortTagsByDescending(tags)

            val releaseBranchHeadCommit = revWalk.parseCommit(releaseBranchRef.objectId);
            return sortedTags.find { tag ->
                if (regex.matches(tag.name)) {
                    val revTag = revWalk.parseCommit(tag.objectId)
                    revWalk.isReachableFrom(releaseBranchHeadCommit, revTag.id)
                } else {
                    false
                }
            }
        }
    }

    fun createTag(git: Git, tagName: String, tagMessage: String, revObj: RevObject) {
        git.tag().setName(tagName)
            .setMessage(tagMessage)
            .setObjectId(revObj)
            .call()
    }

    private fun Repository.getAllTags(): List<Ref> {
        return this.refDatabase.getRefsByPrefix(Constants.R_TAGS)
    }

    private fun RevWalk.isReachableFrom(commit: RevCommit, tagId: ObjectId): Boolean {
        this.markStart(commit)

        return stream(this.spliterator(), true).anyMatch { revCommit ->
            revCommit.id.equals(tagId)
        }
    }

    private fun RevWalk.sortTagsByDescending(tags: List<Ref>): List<Ref> {
        val tagsWithTimestamp = tags.stream().map { unsortedTag ->
            val commit = this.parseCommit(unsortedTag.objectId)
            val commitDate = commit.committerIdent.whenAsInstant
            Pair(unsortedTag, commitDate)
        }.toList()

        val sortedTags = tagsWithTimestamp.sortedByDescending { it.second }.map { it.first }
        return sortedTags
    }
}