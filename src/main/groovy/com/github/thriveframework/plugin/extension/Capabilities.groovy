package com.github.thriveframework.plugin.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

class Capabilities {
    final Property<Boolean> hasApi
    final Property<Boolean> hasSwagger
    final ListProperty<String> websocketsEndpoints
    //todo other/custom capabilities

    @Inject
    Capabilities(ObjectFactory objects) {
        this.hasApi = objects.property(Boolean)
        this.hasSwagger = objects.property(Boolean)
        this.websocketsEndpoints = objects.listProperty(String)
        initDefaults()
    }

    private void initDefaults(){
        hasApi.set true
        hasSwagger.set true
        websocketsEndpoints.set([])
    }

    void api(boolean has=true){
        this.hasApi.set has
    }

    void swagger(boolean has=true){
        this.hasSwagger.set has
    }

    void websocket(String... endpoints){
        websocketsEndpoints.addAll(endpoints)
    }

    Map<String, String> asMap(){
        [
            api: hasApi.get(),
            swagger: hasSwagger.get(),
            websocket: websocketsEndpoints.get().join(",")
        ]
    }
}
