package com.github.thriveframework.plugin.utils

import groovy.transform.Canonical
import org.gradle.api.Project

@Canonical
class ThriveDirectories {
    private final File root
    private final File resources
    private final File metadata

    ThriveDirectories(Project project){
        root = new File(project.buildDir, "thrive")
        resources = new File(root, "resources")
        metadata = new File(root, "metadata")
    }
}
