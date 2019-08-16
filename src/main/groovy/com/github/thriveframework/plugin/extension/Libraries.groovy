package com.github.thriveframework.plugin.extension

import groovy.transform.Canonical
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.inject.Inject

@Slf4j
class Libraries {
    final Property<Boolean> applyManagementPlugin
    final Property<Boolean> useThriveBom

    final Property<Boolean> addLombok
    final Property<Boolean> addThriveCommon

    @Inject
    Libraries(Project project) {
        applyManagementPlugin = project.objects.property(Boolean)
        useThriveBom = project.objects.property(Boolean)
        addLombok = project.objects.property(Boolean)
        addThriveCommon = project.objects.property(Boolean)
        initDefaults()
    }

    private void initDefaults(){
        applyManagementPlugin.set true
        useThriveBom.set true
        addLombok.set true
        addThriveCommon.set true
    }

    void thriveBom(boolean use = true){
        log.info("Setting useThriveBom to $use")
        useThriveBom.set use
    }

    void manage(boolean doIt = true){
        log.info("Setting applyManagementPlugin to $doIt")
        applyManagementPlugin.set doIt
    }

    void lombok(boolean use = true){
        log.info("Setting addLombok to $use")
        addLombok.set use
    }

    void thriveCommon(boolean use = true){
        log.info("Setting addThriveCommon to $use")
        addThriveCommon.set use
    }
}
