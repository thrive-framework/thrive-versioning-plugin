package com.github.thriveframework.plugin.utils

import org.gradle.api.Project

class Projects {
    static String fullName(Project project){
        def l = []
        while (project) {
            l = [ project.name ] + l
            project = project.parent
        }
        ([""]+l).join(":")
    }
}
