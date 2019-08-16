package com.github.thriveframework.plugin.extension

import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.provider.Property

@Slf4j
class ThriveExtension {
    final Capabilities capabilities
    final Libraries libraries

    final Property<String> mainClassName

    final Property<Boolean> useGitBasedVersioning

    final Property<Boolean> isRunnableProject

    ThriveExtension(Project project){
        capabilities = project.objects.newInstance(Capabilities, project)
        libraries = project.objects.newInstance(Libraries, project)

        mainClassName = project.objects.property(String)

        useGitBasedVersioning = project.objects.property(Boolean)
        isRunnableProject = project.objects.property(Boolean)
        initDefaults()
    }

    private void initDefaults(){
        gitBasedVersioning(true)
        service(true)
    }

    void service(boolean is = true){
        isRunnableProject.set is
    }

    void library(){
        service(false)
    }

    void gitBasedVersioning(boolean use = true){
        useGitBasedVersioning.set use
    }

    void libraries(Closure c){
        this.libraries.with c
    }

    void capabilities(Closure c){
        this.capabilities.with c
    }
}
