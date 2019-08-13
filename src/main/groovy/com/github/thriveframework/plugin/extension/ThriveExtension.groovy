package com.github.thriveframework.plugin.extension

import org.gradle.api.Project

class ThriveExtension {
    final Capabilities capabilities

    ThriveExtension(Project project){
        capabilities = project.objects.newInstance(Capabilities)
    }
}
