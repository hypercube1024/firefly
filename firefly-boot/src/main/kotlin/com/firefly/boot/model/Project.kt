package com.firefly.boot.model

import com.beust.jcommander.Parameter
import com.firefly.boot.model.PropertiesLoader.properties
import com.firefly.kotlin.ext.annotation.NoArg
import com.firefly.utils.ProjectVersion
import java.util.*

/**
 * @author Pengtao Qiu
 */
@NoArg
data class Project(
    @Parameter(names = ["--groupId", "-g"], description = "The project group id.", required = true, order = 1)
    var groupId: String?,

    @Parameter(names = ["--artifactId", "-a"], description = "The project artifact id.", required = true, order = 2)
    var artifactId: String?,

    @Parameter(
        names = ["--packageName", "-p"],
        description = "The project package name, e.g., com.xxx.yyy .",
        required = true,
        order = 3
              )
    var packageName: String?,

    @Parameter(
        names = ["--domainName", "-d"],
        description = "The project domain name, e.g., yyy.xxx.com .",
        required = true,
        order = 4
              )
    var domainName: String?,

    @Parameter(names = ["--jarName", "-j"], description = "The project jar name.", required = true, order = 5)
    var uberJarName: String?,

    @Parameter(
        names = ["--template", "-t"],
        description = "The scaffold template name, the value is firefly-web-seed or firefly-simple-seed ",
        order = 6
              )
    var template: String = "firefly-web-seed",

    @Parameter(
        names = ["--outputPath", "-o"],
        description = "The project output path, current path is default.",
        order = 7
              )
    var outputPath: String = ".",

    @Parameter(
        names = ["--buildTool", "-b"],
        description = "The build tool name, the value is maven or gradle.",
        order = 100
              )
    var buildTool: String = BuildTool.MAVEN.value,

    @Parameter(names = ["--help", "-h"], description = "Show the firefly cli usage.", help = true, order = 101)
    var help: Boolean = false,

    @Parameter(names = ["--version", "-v"], description = "Show the firefly cli version.", help = true, order = 102)
    var version: Boolean = false,

    val kotlinVersion: String = properties.getProperty("kotlin.version"),
    val kotlinCoroutineVersion: String = properties.getProperty("kotlin.coroutine.version"),
    val dokkaVersion: String = properties.getProperty("dokka.version"),
    val jacksonVersion: String = properties.getProperty("jackson.version"),
    val redissonVersion: String = properties.getProperty("redisson.version"),
    val fireflyVersion: String = ProjectVersion.getValue()
                  ) {

    constructor() : this(null, null, null, null, null)
}

enum class BuildTool(val value: String) {
    MAVEN("maven"), GRADLE("gradle");

    companion object {
        fun from(value: String): BuildTool {
            return BuildTool.values().find { it.value == value }
                ?: throw IllegalArgumentException("the build tool is not found")
        }
    }

}

object PropertiesLoader {
    val properties: Properties by lazy {
        loadProperties("/dependency.properties")
    }

    private fun loadProperties(path: String): Properties {
        return Project::class.java.getResourceAsStream(path).use {
            val properties = Properties()
            properties.load(it)
            properties
        }
    }
}

