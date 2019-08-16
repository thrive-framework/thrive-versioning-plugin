package com.github.thriveframework.plugin

import com.github.thriveframework.plugin.extension.ThriveExtension
import com.github.thriveframework.plugin.task.EchoTask
import com.github.thriveframework.plugin.task.WriteCapabilitiesTask
import com.gorylenko.GitPropertiesPlugin
import groovy.util.logging.Slf4j
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.util.GradleVersion
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.unbrokendome.gradle.plugins.gitversion.GitVersionPlugin

import java.time.LocalDateTime
import java.time.ZoneId

import static com.github.thriveframework.plugin.utils.Projects.fullName

@Slf4j
class ThrivePlugin implements Plugin<Project> {

    private ThriveExtension extension
    //todo move to ThriveFiles
    private File thriveDir
    private File thriveResourcesDir
    private File thriveMetadataDir

    @Override
    void apply(Project target) {
        verifyGradleVersion()

        thriveDir = new File(target.buildDir, "thrive")
        thriveResourcesDir = new File(thriveDir, "resources")
        thriveMetadataDir = new File(thriveDir, "metadata")

        configureExtensions(target)

        configureJavaLibrary(target)

        target.afterEvaluate { project ->
            configureVersioning(project)

            configureRepositories(project)

            configureDirs(project)

            configureDependencies(project)

            configureGitProperties(project)

            //todo configure artifacts

            configureSpringBoot(project)

            configureProjectTasks(project)

            configureDockerTasks(project)
        }
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0) {
            throw new GradleException("Thrive plugin requires Gradle 5.0 or later. The current version is "
                + GradleVersion.current());
        }
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

    private void configureExtensions(Project project){
        log.info("Creating Thrive extenion in project ${fullName(project)}")
        this.extension = project.extensions.create("thrive", ThriveExtension, project)
    }

    private void configureJavaLibrary(Project project){
        applyPluginIfNeeded(project, JavaLibraryPlugin)
    }

    private void configureVersioning(Project project){
        if (extension.useGitBasedVersioning.get()){

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
                        //todo this fixates x.x.0 versions; check whether major and minor are the same and patch is bigger from tagged instead
                        assert "$version" == "$candidate", "Current development branch should be $version, but is ${candidate}! (context: $versioningContext; tagName: ${versioningContext.tag.tagName})"
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
    }

    private void configureRepositories(Project project){
        log.info("Adding Maven Central, JFrog (snapshot and release), Spring milestone and JitPack repositories to project ${fullName(project)}")
        project.repositories {
            mavenCentral()
            //todo jcenter?
            maven { url 'http://oss.jfrog.org/artifactory/oss-snapshot-local/' }
            maven { url 'http://oss.jfrog.org/artifactory/oss-release-local/' }
            maven { url 'https://repo.spring.io/milestone' }

            maven {
                name "jitpack"
                url "https://jitpack.io"
            }
        }
    }

    private void configureSpringBoot(Project project){
        if (extension.isRunnableProject.get()) {
            log.info "Configuring project ${fullName(project)} as runnable service"
            applyPluginIfNeeded(project, SpringBootPlugin)

            //fixme main class for booJar needs to ne given explicitly;
            // it can be resolved automatically for sure, Spring Cloud replaces Spring Boot
            // annotation somehow, so we should be able to replace it with Thrive annotation
            // as well

            project.springBoot {
                if (extension.mainClassName.isPresent()) {
                    mainClassName = extension.mainClassName.get()
                    log.info("Used ${extension.mainClassName.get()} as main class name")
                } else {
                    log.warn("Main class name not configured! This may break Spring Boot!")
                }
                buildInfo {
                    //todo it would be nice to inject git and build data to manifest as well
                    def tz = ZoneId.systemDefault();
                    def now = LocalDateTime.now().atZone(tz)
                    properties {
                        time = now.toInstant()
                        additional = [
                            timezone : tz.toString(),
                            timestamp: now.toEpochSecond()
                        ]
                    }
                }
            }

            project.bootRun {
                systemProperty "spring.profiles.active", "local"
            }
        } else {
            log.info("Configuring project ${fullName(project)} as a library")
        }
    }

    private void configureGitProperties(Project project){
        applyPluginIfNeeded(project, GitPropertiesPlugin)
    }

    private void configureDirs(Project project){
        log.info("Configuring Thrive resources directory, adding it to main source set in project ${fullName(project)}")
        project.ext {
            thriveDir = thriveDir
            thriveResourcesDir = thriveResourcesDir
        }

        project.sourceSets {
            main {
                resources {
                    srcDir thriveResourcesDir
                }
            }
        }
    }

    private void configureDependencies(Project project){
        if (extension.libraries.applyManagementPlugin.get()) {
            applyPluginIfNeeded(project, DependencyManagementPlugin)
        }

        if (extension.libraries.useThriveBom.get()) {
            log.info("Using Thrive BOM with version 0.3.0-SNAPSHOT")
            project.dependencyManagement {
                imports {
                    //todo: where to take version from? extension, I suppose, with default in plugin properties
                    mavenBom "com.github.thrive-framework:thrive-bom:0.3.0-SNAPSHOT"
                }
            }
        }

        project.dependencies {
            //fixme these configs may not exist!
            if (extension.libraries.addLombok.get()) {
                log.info("Adding Lombok dependencies for ${fullName(project)}")
                compileOnly('org.projectlombok:lombok')
                annotationProcessor('org.projectlombok:lombok')
            }

            if (extension.libraries.addThriveCommon.get()) {
                log.info("Adding thrive-common dependency for ${fullName(project)}")
                implementation "com.github.thrive-framework.thrive:thrive-common"
            }
        }
    }

    private void configureProjectTasks(Project project){
        log.info("Creating 'writeVersion' task in project ${fullName(project)}")
        //todo group, description

        project.tasks.create("writeVersion", EchoTask) {
            content = "${project.version}"
            target = new File(thriveMetadataDir, "version.txt")
        }

        log.info("Creating 'writeCapabilities' task in project ${fullName(project)}")
        project.tasks.create("writeCapabilities", WriteCapabilitiesTask) {
            capabilities = extension.capabilities
            outputFile = new File(thriveResourcesDir, "META-INF/capabilities.properties")
            comment = "Created by ${fullName(project)}:writeCapabilities on behalf of Thrive"
        }

        project.processResources.dependsOn project.writeCapabilities
    }

    private void configureDockerTasks(Project project){
        //dockerfile
        //docker compose todo add to extension
    }
}
