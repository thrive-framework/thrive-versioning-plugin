package com.github.thriveframework.plugin.extension

class Capabilities {
    private boolean hasApi = true
    private boolean hasSwagger = true
    private List<String> websocketsEndpoints = [] as LinkedList
    //todo other/custom capabilities

    void api(boolean hasApi=true){
        this.hasApi = hasApi
    }

    void swagger(boolean hasSwagger=true){
        this.hasSwagger = hasSwagger
    }

    void websocket(String... endpoints){
        websocketsEndpoints.addAll(endpoints)
    }
}
