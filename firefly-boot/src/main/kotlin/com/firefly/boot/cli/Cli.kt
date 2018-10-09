package com.firefly.boot.cli

import com.beust.jcommander.JCommander
import com.firefly.boot.model.Project
import com.firefly.boot.service.impl.KotlinWebScaffoldServiceImpl
import com.firefly.utils.log.LogFactory
import com.firefly.utils.time.Millisecond100Clock

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) = runCommand(args)

fun runCommand(args: Array<String>) {
    try {
        val project = Project()
        JCommander.newBuilder().addObject(project).build().parse(*args)
        val service = KotlinWebScaffoldServiceImpl()
        service.generate(project)
    } finally {
        LogFactory.getInstance().stop()
        Millisecond100Clock.stop()
    }
}