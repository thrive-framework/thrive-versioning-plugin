package com.github.thriveframework.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

@CacheableTask
class Echo extends DefaultTask {
    @Input
    Property<String> content

    @OutputFile
    final RegularFileProperty target

    @Inject
    Echo(ObjectFactory objects) {
        content = objects.property(String)
        target = objects.fileProperty()
    }

    @TaskAction
    void run(){
        target.asFile.get().parentFile.mkdirs()
        target.get().asFile.text = content.get()
    }
}
