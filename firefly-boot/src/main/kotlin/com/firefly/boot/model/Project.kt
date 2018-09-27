package com.firefly.boot.model

/**
 * @author Pengtao Qiu
 */
data class Project(
    var path: String,
    var groupId: String,
    var artifactId: String,
    var packageName: String,
    var uberJarName: String,
    var buildTool: String = BuildTool.MAVEN.value
                  )

enum class BuildTool(val value: String) {
    MAVEN("maven"), GRADLE("gradle");

    companion object {
        fun from(value: String): BuildTool {
            return BuildTool.values().find { it.value == value }
                ?: throw IllegalArgumentException("the build tool is not found")
        }
    }

}