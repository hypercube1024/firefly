package com.firefly.boot.service.impl

import com.firefly.boot.model.BuildTool
import com.firefly.boot.model.Project
import com.firefly.boot.service.ScaffoldService
import com.firefly.utils.Assert
import com.firefly.utils.io.FileUtils
import com.github.mustachejava.DefaultMustacheFactory
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author Pengtao Qiu
 */
class KotlinWebScaffoldServiceImpl : ScaffoldService {

    private val mustacheFactory = DefaultMustacheFactory()

    override fun generate(project: Project) {
        val projectDir = Paths.get(project.path)
        println("generate project -> $project")

        Assert.isTrue(Files.exists(projectDir), "The directory does not exist")
        Assert.isTrue(Files.isDirectory(projectDir), "The path is not a directory")

        // init project dir
        FileUtils.delete(projectDir)
        Files.createDirectories(projectDir)
        val templatePath = "/project_template/firefly-web-seed"
        val templateDir = toPath(templatePath)
        val resourcesDir = toPath("/")
        val templateSuffix = ".mustache"
        FileUtils.filter(templateDir, "*$templateSuffix") { path ->
            val templateName = path.toString().substring(resourcesDir.toString().length + 1)
            val fileName = path.fileName.toString()
            val newFileName = fileName.substring(0, fileName.length - templateSuffix.length)

            when {
                path.toString().endsWith(Paths.get("/firefly-web-seed/pom.xml.mustache").toString())
                        || path.toString().endsWith(Paths.get("/firefly-web-seed/api/pom.xml.mustache").toString())
                        || path.toString().endsWith(Paths.get("/firefly-web-seed/common/pom.xml.mustache").toString())
                        || path.toString().endsWith(Paths.get("/firefly-web-seed/server/pom.xml.mustache").toString()) -> {
                    println("[root]:$path")

                    val dir = Paths.get(projectDir.toString(), templateName.substring(templatePath.length, templateName.length - fileName.length))
                    val outputFile = File(dir.toString(), newFileName)
                    createOutputFile(outputFile)
                    println("[root:create]$outputFile")

                    FileWriter(outputFile).use { mustacheFactory.compile(templateName).execute(it, project) }
                    println("[root:generate]$outputFile")
                }
                path.toString().contains(Paths.get("/firefly-web-seed/server").toString()) -> {
                    println("[server]:$path")
                }
                path.toString().contains(Paths.get("/firefly-web-seed/api").toString()) -> {
                    println("[api]:$path")
                }
                path.toString().contains(Paths.get("/firefly-web-seed/common").toString()) -> {
                    println("[common]:$path")
                }
            }
        }

        when (BuildTool.from(project.buildTool)) {
            BuildTool.MAVEN -> {

            }
            BuildTool.GRADLE -> {

            }
        }
    }

    private fun toPath(path: String) = Paths.get(KotlinWebScaffoldServiceImpl::class.java.getResource(path).toURI())

    private fun createOutputFile(outputFile: File) {
        if (!outputFile.exists()) {
            val parent = outputFile.parentFile
            val success = parent.mkdirs()
            if (success)
                println("create dir " + parent.absolutePath + " success")
        }
    }
}