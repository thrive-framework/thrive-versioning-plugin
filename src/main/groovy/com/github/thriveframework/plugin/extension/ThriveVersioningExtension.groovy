package com.github.thriveframework.plugin.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject

class ThriveVersioningExtension {
    final Property<Boolean> configurePublishing
    final Property<Boolean> configureForJitpack

    @Inject
    ThriveVersioningExtension(ObjectFactory objects){
        configurePublishing = objects.property(Boolean)
        configureForJitpack = objects.property(Boolean)
        initConventions()
    }

    private void initConventions(){
        configurePublishing.convention(true)
        configureForJitpack.convention(true)
    }
}
