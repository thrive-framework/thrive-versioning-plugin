package com.github.thriveframework.plugin

import com.gorylenko.GitPropertiesPlugin
import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.springframework.boot.gradle.plugin.SpringBootPlugin

import java.time.LocalDateTime
import java.time.ZoneId

class ThriveServicePlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        verifyGradleVersion()

        configureExtensions(target)

        target.apply plugin: 'java-library'

        configureRepositories(target)

        configureSpringBoot(target)
        configureGitProperties(target)

        configureDirs(target)

        configureDependencies(target)

        configureProjectTasks(target)

        //todo configure artifacts
        configureDockerTasks(target)
    }

    private void verifyGradleVersion() {
        if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) < 0) {
            throw new GradleException("Thrive plugin requires Gradle 5.0 or later." + " The current version is "
                + GradleVersion.current());
        }
    }

    private void configureRepositories(Project project){
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
        project.plugins.apply(SpringBootPlugin)

        project.springBoot { buildInfo {
                //todo it would be nice to inject git and build data to manifest as well
                def tz = ZoneId.systemDefault();
                def now = LocalDateTime.now().atZone(tz)
                properties {
                    time = now.toInstant()
                    additional = [
                        timezone: tz.toString(),
                        timestamp: now.toEpochSecond()
                    ]
                }
            } }

        project.bootRun {
            systemProperty "spring.profiles.active", "local"
        }
    }

    private void configureGitProperties(Project project){
        if (!project.plugins.hasPlugin(GitPropertiesPlugin))
            project.plugins.apply(GitPropertiesPlugin)
    }

    private void configureDirs(Project project){
        def thriveDir = new File(project.buildDir, "thrive")
        def thriveResourcesDir = new File(thriveDir, "resources")
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

    private void configureExtensions(Project project){
        project.extensions.create("thrive", com.github.thriveframework.plugin.extension.ThriveExtension, project)
    }

    private void configureDependencies(Project project){
        project.plugins.apply(DependencyManagementPlugin)

        project.dependencyManagement {
            imports {
                //todo: where to take version from? extension, I suppose, with default in plugin properties
                mavenBom "com.github.thrive-framework:thrive-bom:0.3.0-SNAPSHOT"
            }
        }

        project.dependencies {
            //fixme these configs may not exist!
            //todo should be configurable via extension
            compileOnly('org.projectlombok:lombok')
            annotationProcessor('org.projectlombok:lombok')

            //todo take it from BOM
            implementation "com.github.thrive-framework.thrive:thrive-common:0.3.0-SNAPSHOT"
        }
    }

    private void configureProjectTasks(Project project){
        //dump version
        //dump capabilities; dependency for processResources
    }

    private void configureDockerTasks(Project project){
        //dockerfile
        //docker compose todo add to extension
    }
}
