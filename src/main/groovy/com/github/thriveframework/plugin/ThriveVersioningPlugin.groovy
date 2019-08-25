package com.github.thriveframework.plugin

import com.github.thriveframework.plugin.extension.ThriveVersioningExtension
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.unbrokendome.gradle.plugins.gitversion.GitVersionPlugin
import org.unbrokendome.gradle.plugins.gitversion.version.ImmutableSemVersionImpl
import org.unbrokendome.gradle.plugins.gitversion.version.MutableSemVersion
import org.unbrokendome.gradle.plugins.gitversion.version.SemVersion

import static com.github.thriveframework.plugin.task.VersionTasks.createPrintVersion
import static com.github.thriveframework.plugin.task.VersionTasks.createWriteVersion
import static com.github.thriveframework.plugin.utils.Projects.applyPlugin
import static com.github.thriveframework.plugin.utils.Projects.fullName

@Slf4j
class ThriveVersioningPlugin implements Plugin<Project> {
    private ThriveVersioningExtension extension

    @Override
    void apply(Project project) {
        configureExtension(project)
        configureVersioning(project)
        addVersionTasks(project)
        project.afterEvaluate {
            configurePublishing(project)
        }
    }
    
    private void configureExtension(Project project){
        extension = project.extensions.create("thriveVersioning", ThriveVersioningExtension)
    }

    private void configureVersioning(Project project){
        applyPlugin(project, GitVersionPlugin)

        def semverRegex = /(\d+[.]\d+[.]\d+)/
        def versioningContext = [fullyDetermined: false]

        //todo what happens when there is not tag at all?
        project.gitVersion {
            rules {
                before {
                    def tag = findLatestTag(~semverRegex, true)
                    def taggedVersion
                    if (tag)
                        taggedVersion = tag.matches[1]
                    else
                        taggedVersion = "0.0.0"
                    versioningContext.taggedVersion = taggedVersion
                    def commitsSinceTag = tag ? countCommitsSince(tag) : 0
                    versioningContext.commitsSinceTag = commitsSinceTag
                    version = SemVersion.parse(taggedVersion)
                    versioningContext.lastVersion = SemVersion.parse(taggedVersion)
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
                    assert successors.contains("$candidate"), "Current development branch should be one of ${successors}, but is ${candidate}! (context: $versioningContext)"
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
    }

    private List<String> getSuccessors(SemVersion version){
        [
            "${new ImmutableSemVersionImpl(version.major+1, 0, 0, "", "")}",
            "${new ImmutableSemVersionImpl(version.major, version.minor+1, 0, "", "")}",
            "${new ImmutableSemVersionImpl(version.major, version.minor, version.patch+1, "", "")}",
        ]
    }

    private void addVersionTasks(Project project){
        createWriteVersion(project)
        def versionPrefix = "Project ${fullName(project)} version: "
        def statePrefix =   "Project ${fullName(project)} state:   "
        createPrintVersion(project, versionPrefix).doLast {
            println "$statePrefix ${project.projectState}"
        }
    }

    private void configurePublishing(Project project){
        if (extension.configurePublishing.get()){
            log.info("Configuring publishing")
            applyPlugin(project, 'maven-publish')

            project.sourceCompatibility = 1.8
            project.targetCompatibility = 1.8

            project.tasks.create("sourcesJar", Jar) {
                archiveClassifier = 'sources'
                from project.sourceSets.main.allSource
            }

            project.tasks.create("javadocJar", Jar) {
                archiveClassifier = 'javadoc'
                from project.javadoc.destinationDir
            }

            project.publishing {
                publications {
                    main(MavenPublication) {
                        from project.components.java

                        artifact project.sourcesJar
                        artifact project.javadocJar
                    }
                }
            }

            configureJitpack(project)
        } else {
            log.info("Publishing nor JitPack not configured")
        }
    }

    private void configureJitpack(Project project){
        if (extension.configureForJitpack.get()){
            log.info("Configuring JitPack")
            project.publishMainPublicationToMavenLocal.dependsOn project.build

            //todo this feature is broken https://github.com/thrive-framework/thrive-versioning-plugin/issues/7
//            if (project.parent) {// this is not a root project
                //change group so that it matches JitPack convention for multi-project
                // builds
                // see: https://jitpack.io/docs/BUILDING/#multi-module-projects
                //todo does assumption rootProject.name == repository name hold?
//                project.group = "${project.rootProject.group}.${project.rootProject.name}"

                //todo this solution won't take more than two levels into account
                // can gradle even support multi-level project structure?
//            }
        } else {
            log.info("JitPack not configured")
        }
    }
}
