package com.github.thriveframework.plugin.extension

import com.github.thriveframework.plugin.utils.Defaults
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject

class Dockerfile {
    final Property<String> baseImage
    final ListProperty<String> exposes
    final Property<String> name
    final Property<String> maintainer
    final Property<String> version
    final MapProperty<String, String> labels
    final Property<String> workdir

    @Inject
    Dockerfile(Project project, ProviderFactory providerFactory){ //we need to retrieve project props, so we inject it instead of factory
        ObjectFactory objects = project.objects
        baseImage = objects.property(String)
        exposes = objects.listProperty(String)
        name = objects.property(String)
        maintainer = objects.property(String)
        version = objects.property(String)
        labels = objects.mapProperty(String, String)
        workdir = objects.property(String)
        initDefaults(project, providerFactory)
    }

    private void initDefaults(Project project, ProviderFactory providerFactory){
        baseImage.set Defaults.baseImage
        exposes.set(["8080"])
        name.set "${project.name}"
        version.set providerFactory.provider({ "${project.version}" })
        labels.set([:])
        workdir.set("/var/opt/${project.name}")
    }

    //todo add fluent setters
}
