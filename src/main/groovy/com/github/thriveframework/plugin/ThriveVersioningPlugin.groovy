package com.github.thriveframework.plugin

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.unbrokendome.gradle.plugins.gitversion.GitVersionPlugin
import org.unbrokendome.gradle.plugins.gitversion.version.ImmutableSemVersionImpl
import org.unbrokendome.gradle.plugins.gitversion.version.MutableSemVersion
import org.unbrokendome.gradle.plugins.gitversion.version.SemVersion

import static com.github.thriveframework.plugin.utils.Projects.fullName

@Slf4j
class ThriveVersioningPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        configureVersioning(project)
    }


    private void applyPluginIfNeeded(Project project, Class plugin){
        String pluginClassName = plugin.canonicalName
        log.info("Trying to apply plugin with implementation $pluginClassName to project ${fullName(project)}")
        if (!project.plugins.findPlugin(plugin)) {
            log.info("Applying $pluginClassName")
            project.apply plugin: plugin
        } else {
            log.info("$pluginClassName already applied")
        }
    }

    private void configureVersioning(Project project){
        applyPluginIfNeeded(project, GitVersionPlugin)

        def semverRegex = /(\d+[.]\d+[.]\d+)/
        def versioningContext = [fullyDetermined: false]

        //todo what happens when there is not tag at all?
        project.gitVersion {
            rules {
                before {
                    def tag = findLatestTag(~semverRegex, true)
                    versioningContext.tag = tag
                    def commitsSinceTag = countCommitsSince tag
                    versioningContext.commitsSinceTag = commitsSinceTag
                    version = tag.matches[1]
                    versioningContext.lastVersion = SemVersion.parse("$version")
                    if (commitsSinceTag > 0 || branchName != "master")
                        version.incrementMinor()
                }
                onBranch("master"){
                    if (versioningContext.commitsSinceTag > 0) {
                        version.prereleaseTag = "RC"
                        project.ext {
                            projectState = "RC"
                        }
                    } else {
                        project.ext {
                            projectState = "RELEASE"
                        }
                    }
                    versioningContext.fullyDetermined = true
                }
                onBranch(~semverRegex) {
                    def candidate = matches[1]
                    def successors = getSuccessors(versioningContext.lastVersion)
                    log.debug("Acceptable version successors: $successors")
                    assert successors.contains("$candidate"), "Current development branch should be one of ${successors}, but is ${candidate}! (context: $versioningContext; tagName: ${versioningContext.tag.tagName})"
                    version = MutableSemVersion.parse(candidate)
                    version.prereleaseTag = "SNAPSHOT"
                    project.ext {
                        projectState = "SNAPSHOT"
                    }
                    versioningContext.fullyDetermined = true
                }
                after {
                    if (!versioningContext.fullyDetermined){
                        version.prereleaseTag = "FEATURE"
                        version.buildMetadata = "${branchName.hashCode()}_${versioningContext.commitsSinceTag}"
                        versioningContext.fullyDetermined = true
                        project.ext {
                            projectState = "FEATURE"
                        }
                    }
                }
            }
        }

        project.version = project.gitVersion.determineVersion()

        log.info "Project \"${fullName(project)}\" version: ${project.version}"
        log.info "Project \"${fullName(project)}\" state:   ${project.projectState}"
    }

    private List<String> getSuccessors(SemVersion version){
        [
            "${new ImmutableSemVersionImpl(version.major+1, 0, 0, "", "")}",
            "${new ImmutableSemVersionImpl(version.major, version.minor+1, 0, "", "")}",
            "${new ImmutableSemVersionImpl(version.major, version.minor, version.patch+1, "", "")}",
        ]
    }
}
