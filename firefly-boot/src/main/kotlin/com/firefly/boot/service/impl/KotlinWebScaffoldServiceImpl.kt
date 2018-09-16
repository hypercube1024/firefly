package com.firefly.boot.service.impl

import com.firefly.boot.model.BuildTool
import com.firefly.boot.model.Project
import com.firefly.boot.service.ScaffoldService
import com.firefly.utils.Assert
import com.firefly.utils.io.FileUtils
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author Pengtao Qiu
 */
class KotlinWebScaffoldServiceImpl : ScaffoldService {

    override fun generate(project: Project) {
        val projectDir = Paths.get(project.path)
        println("generate project -> $project")

        Assert.isTrue(Files.exists(projectDir), "The directory does not exist")
        Assert.isTrue(Files.isDirectory(projectDir), "The path is not a directory")

        // init project dir
        FileUtils.delete(projectDir)
        Files.createDirectories(projectDir)
        listOf("${project.artifactId}-api",
                "${project.artifactId}-common",
                "${project.artifactId}-server")
                .map { Paths.get(projectDir.toAbsolutePath().toString(), it) }
                .forEach { moduleDir ->
                    Files.createDirectory(moduleDir)
                    println("create module directory -> $moduleDir")

                    val srcDir = Paths.get(moduleDir.toString(), "src")
                    Files.createDirectory(srcDir)
                    println("create src directory -> $srcDir")

                    listOf(Paths.get(srcDir.toString(), "main", "java"),
                            Paths.get(srcDir.toString(), "main", "kotlin"),
                            Paths.get(srcDir.toString(), "test", "java"),
                            Paths.get(srcDir.toString(), "test", "kotlin")).forEach { srcPath ->
                        Files.createDirectories(srcPath)
                        println("create src directory -> $srcPath")

                        
                    }
                }

        when (BuildTool.from(project.buildTool)) {
            BuildTool.MAVEN -> {

            }
            BuildTool.GRADLE -> {

            }
        }
    }
}