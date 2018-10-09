package com.firefly.boot.model

import com.beust.jcommander.Parameter
import com.firefly.kotlin.ext.annotation.NoArg

/**
 * @author Pengtao Qiu
 */
@NoArg
data class Project(
    @Parameter(names = ["--groupId", "-g"], description = "The project group id.")
    var groupId: String,

    @Parameter(names = ["--artifactId", "-a"], description = "The project artifact id.")
    var artifactId: String,

    @Parameter(names = ["--packageName", "-p"], description = "The project package name, e.g., com.xxx.yyy .")
    var packageName: String,

    @Parameter(names = ["--domainName", "-d"], description = "The project domain name, e.g., yyy.xxx.com .")
    var domainName: String,

    @Parameter(names = ["--jarName", "-j"], description = "The project jar name.")
    var uberJarName: String = domainName,

    @Parameter(names = ["--outputPath", "-o"], description = "The project output path, current path is default.")
    var outputPath: String = ".",

    @Parameter(names = ["--fireflyVersion", "-f"], description = "The firefly version.")
    var fireflyVersion: String = "4.9.1-SNAPSHOT",

    @Parameter(names = ["--buildTool", "-b"], description = "The build tool name, maven or gradle.")
    var buildTool: String = BuildTool.MAVEN.value
                  ) {

    constructor() : this("", "", "", "")
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