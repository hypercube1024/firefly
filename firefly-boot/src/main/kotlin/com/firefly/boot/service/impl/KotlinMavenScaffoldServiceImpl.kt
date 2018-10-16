package com.firefly.boot.service.impl

import com.firefly.boot.model.Project
import com.firefly.boot.service.ScaffoldService
import com.firefly.kotlin.ext.log.KtLogger
import com.firefly.utils.Assert
import com.firefly.utils.io.FileUtils
import com.github.mustachejava.DefaultMustacheFactory
import java.io.File
import java.io.FileWriter
import java.net.JarURLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Pengtao Qiu
 */
class KotlinMavenScaffoldServiceImpl(private val template: String = "firefly-web-seed") : ScaffoldService {

    private val log = KtLogger.getLogger { }

    private val mustacheFactory = DefaultMustacheFactory()
    private val fileSeparator = System.getProperty("file.separator")
    private val osName = System.getProperty("os.name")
    private val templatePackage = mapOf(
        "firefly-web-seed" to "/com/firefly/kt/web/seed",
        "firefly-simple-seed" to "/com/firefly/kt/simple/seed"
                                       )

    override fun generate(project: Project) {
        val startTime = System.currentTimeMillis()
        val projectDir = if (project.outputPath != ".") {
            Paths.get(project.outputPath, project.artifactId)
        } else {
            val file = File("")
            Paths.get(file.absolutePath, project.artifactId)
        }
        log.info("os -> $osName")
        log.info("project dir -> $projectDir")
        log.info("generate project -> $project")

        if (!Files.exists(projectDir)) {
            Files.createDirectories(projectDir)
        } else {
            Assert.isTrue(Files.isDirectory(projectDir), "The path is not a directory")
        }

        // init project dir
        FileUtils.delete(projectDir)
        Files.createDirectories(projectDir)
        val templatePath = "/project_template/$template"
        val templateSuffix = ".mustache"
        val fileNamePattern = "*$templateSuffix"
        val url = KotlinMavenScaffoldServiceImpl::class.java.getResource(templatePath)

        if (url.toString().startsWith("jar:")) {
            val jarCon = url.openConnection() as JarURLConnection
            val jarFile = jarCon.jarFile
            FileUtils.filterInJar(jarFile, fileNamePattern) { entry ->
                val templateName = entry.name
                render(templateName, templateName, templateSuffix, templatePath, projectDir, project)
            }
        } else {
            val templateDir = toPath(templatePath)
            val resourcesDir = templateDir.parent.parent

            FileUtils.filter(templateDir, fileNamePattern) { path ->
                val currentPath = path.toString()
                val templateName = currentPath.substring(resourcesDir.toString().length + 1)
                render(templateName, currentPath, templateSuffix, templatePath, projectDir, project)
            }
        }

        val endTime = System.currentTimeMillis()
        log.info("generate project successfully. -> total time: ${endTime - startTime}ms, output path: $projectDir")
    }

    private fun render(
        templateName: String,
        currentPath: String,
        templateSuffix: String,
        templatePath: String,
        projectDir: Path,
        project: Project
                      ) {
        val fileName = Paths.get(templateName).fileName.toString()
        val newFileName = fileName.substring(0, fileName.length - templateSuffix.length)
        val newPackagePath = Paths.get("/", project.packageName!!.replace('.', '/')).toString()
        val templatePackagePath = Paths.get(templatePackage[template]).toString()
        val outputDir = Paths.get(
            projectDir.toString(),
            templateName.substring(templatePath.length, templateName.length - fileName.length)
                                 ).toString()

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

        FileWriter(outputFile).use {
            if (osName.toLowerCase().contains("windows")) {
                mustacheFactory.compile(templateName.replace(fileSeparator, "/")).execute(it, project)
            } else {
                mustacheFactory.compile(templateName).execute(it, project)
            }
        }
        log.info("generate -> $outputFile")
    }

    private fun isFilter(currentPath: String): Boolean {
        return currentPath.contains(Paths.get("/src/main/filters").toString())
    }

    private fun isResource(currentPath: String): Boolean {
        return currentPath.contains(Paths.get("/src/main/resources").toString()) || currentPath.contains(Paths.get("/src/test/resources").toString())
    }

    private fun isSource(currentPath: String): Boolean {
        return currentPath.endsWith(".java.mustache") || currentPath.endsWith(".kt.mustache")
    }

    private fun isRoot(currentPath: String): Boolean {
        return currentPath.endsWith(Paths.get("/$template/pom.xml.mustache").toString())
                || currentPath.endsWith(Paths.get("/$template/api/pom.xml.mustache").toString())
                || currentPath.endsWith(Paths.get("/$template/common/pom.xml.mustache").toString())
                || currentPath.endsWith(Paths.get("/$template/server/pom.xml.mustache").toString())
    }

    private fun toPath(path: String) = Paths.get(KotlinMavenScaffoldServiceImpl::class.java.getResource(path).toURI())

    private fun createOutputDir(outputFile: File) {
        if (!outputFile.exists()) {
            val parent = outputFile.parentFile
            parent.mkdirs()
        }
    }
}