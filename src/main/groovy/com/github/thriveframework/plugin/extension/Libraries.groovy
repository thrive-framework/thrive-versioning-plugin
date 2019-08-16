package com.github.thriveframework.plugin.extension

import com.github.thriveframework.plugin.utils.Defaults
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject

//maybe "dependencies" can work somehow?
class Libraries {
    final Property<Boolean> applyManagementPlugin
    final Property<Boolean> useThriveBom
    final Property<String> bomVersion

    final Property<Boolean> addLombok
    final Property<Boolean> addThriveCommon

    @Inject
    Libraries(ObjectFactory objects) {
        applyManagementPlugin = objects.property(Boolean)
        useThriveBom = objects.property(Boolean)
        bomVersion = objects.property(String)
        addLombok = objects.property(Boolean)
        addThriveCommon = objects.property(Boolean)
        initDefaults()
    }

    private void initDefaults(){
        manage()
        thriveBom(Defaults.bomVersion)
        lombok()
        thriveCommon()
    }

    void thriveBom(String version){
        thriveBom(true)
        bomVersion.set version
    }

    void thriveBom(boolean use = true){
        useThriveBom.set use
    }

    void manage(boolean doIt = true){
        applyManagementPlugin.set doIt
    }

    void lombok(boolean use = true){
        addLombok.set use
    }

    void thriveCommon(boolean use = true){
        addThriveCommon.set use
    }
}
