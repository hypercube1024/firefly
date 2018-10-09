package com.firefly.boot.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
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
        val commander = JCommander.newBuilder().addObject(project).build()
        commander.programName = "fireflyCli"
        commander.parse(*args)

        if (project.help || project.version) {
            println("The ${commander.programName} version is 4.9.1")
            commander.usage()
        } else {
            val service = KotlinWebScaffoldServiceImpl()
            service.generate(project)
        }
    } catch (e: ParameterException) {
        e.usage()
    } finally {
        LogFactory.getInstance().stop()
        Millisecond100Clock.stop()
    }
}