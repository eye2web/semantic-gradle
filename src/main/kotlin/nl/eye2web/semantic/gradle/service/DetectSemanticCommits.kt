package nl.eye2web.semantic.gradle.service

import nl.eye2web.semantic.gradle.build.configuration.model.BuildConfiguration
import nl.eye2web.semantic.gradle.model.ReleaseVersion
import nl.eye2web.semantic.gradle.model.SemanticChanges
import nl.eye2web.semantic.gradle.model.GitCommit
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import java.util.stream.StreamSupport

class DetectSemanticCommits {

    companion object {
        private const val VERSION_REGEX =
            "(\\d+)\\.(\\d+)\\.(\\d+)(?:-([A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*))?(?:\\+([A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*))?"

        fun getVersionRegex(): Regex = "^$VERSION_REGEX\$".toRegex()
    }

    fun getChangesSincePreviousRelease(
        gitRepo: File,
        buildConfigurationList: List<BuildConfiguration>,
        releaseBranches: List<String>,
        projectName: String,
    ): SemanticChanges {
        val gitService = GitService()

        val repository = gitService.openGitRepo(gitRepo)

        val semVerProjectRegex = getSemverRegex(projectName)

        val branchRef =
            repository.findRef(repository.branch) ?: throw Exception("Branch ${repository.branch} not found.")

        println("Current branch: [${branchRef.name}]")

        releaseBranches.find { releaseBranch -> branchRef.name.endsWith(releaseBranch) }?.let { branch ->
            println("Release branch [$branch] detected")
        } ?: println("No release branch detected. Release branches are $releaseBranches")

        val tagRef = gitService.findLatestTagBy(repository, branchRef, semVerProjectRegex)
            ?: throw Exception("No Tags found for project $projectName. Example tag $projectName-v{MAYOR d+}.{MINOR d+}.{PATCH d+}-{PRERELEASE .*}+{BUILD .*}")

        println("Previous release tag: [${tagRef.name}]")

        RevWalk(repository).use { revWalk ->
            val tagCommit = revWalk.parseCommit(tagRef.objectId)
            val branchCommit = revWalk.parseCommit(branchRef.objectId)

            revWalk.markStart(branchCommit)
            revWalk.markUninteresting(tagCommit)

            val commits = StreamSupport.stream(revWalk.spliterator(), true).filter { commit ->
                DiffFormatter(System.out).use { diffFormatter ->
                    diffFormatter.setRepository(repository)
                    diffFormatter.setContext(0)

                    val parentCommit = commit.getParent(0)

                    // Find changes in specific build project
                    diffFormatter.scan(parentCommit.tree, commit.tree).any { diff ->

                        // Scan root project and all included builds
                        buildConfigurationList.stream().anyMatch { buildInfo ->
                            // Check for configured paths
                            buildInfo.relativePaths.stream().anyMatch { otherPath ->
                                diff.newPath.startsWith(otherPath, true) || diff.newPath.equals(otherPath, true) ||
                                        diff.oldPath.startsWith(otherPath, true) || diff.oldPath.equals(otherPath, true)
                            }
                        }
                    }
                }
            }.map { commit ->
                GitCommit(commit.name, commit.fullMessage.trimEnd { it == '\n' })
            }.toList()

            return SemanticChanges(getPreviousReleaseVersion(projectName, semVerProjectRegex, tagRef), commits)
        }
    }

    private fun getSemverRegex(projectName: String): Regex {
        return "^refs/tags/$projectName-v$VERSION_REGEX\$".toRegex()
    }

    private fun getPreviousReleaseVersion(projectName: String, regex: Regex, tagRef: Ref): ReleaseVersion {
        val result = regex.matchEntire(tagRef.name)!!

        val major = result.groups[1]!!.value.toInt()
        val minor = result.groups[2]!!.value.toInt()
        val patch = result.groups[3]!!.value.toInt()
        val preRelease = result.groups[4]?.value ?: ""
        val build = result.groups[5]?.value ?: ""

        return ReleaseVersion(projectName, major, minor, patch, preRelease, build)
    }

}