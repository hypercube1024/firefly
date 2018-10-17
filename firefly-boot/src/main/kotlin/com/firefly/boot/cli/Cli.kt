package com.firefly.boot.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import com.firefly.boot.model.Project
import com.firefly.boot.service.impl.KotlinMavenScaffoldServiceImpl
import com.firefly.utils.log.LogFactory

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
            println("The ${commander.programName} version is 4.9.2")
            commander.usage()
        } else {
            try {
                val service = KotlinMavenScaffoldServiceImpl(project.template)
                service.generate(project)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                LogFactory.getInstance().stop()
            }
        }
    } catch (e: ParameterException) {
        e.usage()
    }
}