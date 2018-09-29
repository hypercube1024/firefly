package com.firefly.boot.model

/**
 * @author Pengtao Qiu
 */
data class Project(
    var groupId: String,
    var artifactId: String,
    var packageName: String,
    var domainName: String,
    var outputPath: String,
    var uberJarName: String = domainName,
    var fireflyVersion: String = "4.9.1-dev30",
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