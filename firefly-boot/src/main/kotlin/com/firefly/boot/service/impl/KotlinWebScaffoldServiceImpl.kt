package com.firefly.boot.service.impl

import com.firefly.boot.model.Project
import com.firefly.boot.service.ScaffoldService
import com.firefly.utils.Assert
import com.firefly.utils.io.FileUtils
import com.github.mustachejava.DefaultMustacheFactory
import java.io.File
import java.io.FileWriter
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @author Pengtao Qiu
 */
class KotlinWebScaffoldServiceImpl : ScaffoldService {

    private val mustacheFactory = DefaultMustacheFactory()

    override fun generate(project: Project) {
        val projectDir = Paths.get(project.outputPath)
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
            val currentPath = path.toString()
            val templateName = currentPath.substring(resourcesDir.toString().length + 1)
            val fileName = path.fileName.toString()
            val newFileName = fileName.substring(0, fileName.length - templateSuffix.length)
            val newPackagePath = Paths.get("/", project.packageName.replace('.', '/')).toString()
            val templatePackagePath = Paths.get("/com/firefly/kt/web/seed").toString()
            val outputDir = Paths.get(
                projectDir.toString(),
                templateName.substring(templatePath.length, templateName.length - fileName.length)
                                     ).toString()

            println(currentPath)
            val outputFile = when {
                isRoot(currentPath) || isResource(currentPath) || isFilter(currentPath) -> {
                    val outputFile = File(outputDir, newFileName)
                    createOutputDir(outputFile)
                    outputFile
                }
                isSource(currentPath) -> {
                    val outputFile = File(outputDir.replace(templatePackagePath, newPackagePath), newFileName)
                    createOutputDir(outputFile)
                    outputFile
                }
                else -> throw IllegalStateException("the project template error -> $currentPath")
            }

            FileWriter(outputFile).use { mustacheFactory.compile(templateName).execute(it, project) }
            println("generate -> $outputFile")
        }
    }

    private fun isFilter(currentPath: String): Boolean {
        return currentPath.contains("/src/main/filters")
    }

    private fun isResource(currentPath: String): Boolean {
        return currentPath.contains("/src/main/resources") || currentPath.contains("/src/test/resources")
    }

    private fun isSource(currentPath: String): Boolean {
        return currentPath.endsWith(".java.mustache") || currentPath.endsWith(".kt.mustache")
    }

    private fun isRoot(currentPath: String): Boolean {
        return currentPath.endsWith(Paths.get("/firefly-web-seed/pom.xml.mustache").toString())
                || currentPath.endsWith(Paths.get("/firefly-web-seed/api/pom.xml.mustache").toString())
                || currentPath.endsWith(Paths.get("/firefly-web-seed/common/pom.xml.mustache").toString())
                || currentPath.endsWith(Paths.get("/firefly-web-seed/server/pom.xml.mustache").toString())
    }

    private fun toPath(path: String) = Paths.get(KotlinWebScaffoldServiceImpl::class.java.getResource(path).toURI())

    private fun createOutputDir(outputFile: File) {
        if (!outputFile.exists()) {
            val parent = outputFile.parentFile
            val success = parent.mkdirs()
            if (success)
                println("create dir " + parent.absolutePath + " success")
        }
    }
}