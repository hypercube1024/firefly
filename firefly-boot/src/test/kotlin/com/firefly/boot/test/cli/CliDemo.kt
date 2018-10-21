package com.firefly.boot.test.cli

import com.firefly.boot.cli.runCommand
import java.nio.file.Paths

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    val userHome = System.getProperty("user.home")
    val outputPath = Paths.get(userHome, "/Develop/test_project").toString()
    generateWeb(outputPath)
}

fun generateWeb(outputPath: String) {
    val command = arrayOf(
        "-g", "com.fireflysource.apple.tree",
        "-a", "apple-tree-website",
        "-p", "com.fireflysource.apple.tree",
        "-d", "apple.tree.fireflysource.com",
        "-j", "apple.tree.fireflysource.com",
        "-o", outputPath
                         )
    runCommand(command)
}

fun generateSimple(outputPath: String) {
    val command = arrayOf(
        "-g", "com.fireflysource.oj.answer",
        "-a", "pengtao-oj-answer",
        "-p", "com.fireflysource.oj.answer",
        "-d", "answer.oj.fireflysource.com",
        "-j", "answer.oj.fireflysource.com",
        "-o", outputPath,
        "-t", "firefly-simple-seed"
                         )
    runCommand(command)
}