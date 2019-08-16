package com.github.thriveframework.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class EchoTask extends DefaultTask {
    @Input
    final Property<String> content

    @OutputFile
    final RegularFileProperty target

    //fixme I should pass ObjectFactory instead  - everywhere
    @Inject
    EchoTask(ObjectFactory objects) {
        content = objects.property(String)
        target = objects.fileProperty()
    }

    @TaskAction
    void run(){
        target.asFile.get().parentFile.mkdirs()
        target.get().asFile.text = content.get()
    }
}
