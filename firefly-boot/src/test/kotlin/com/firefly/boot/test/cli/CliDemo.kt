package com.firefly.boot.test.cli

import com.firefly.boot.cli.runCommand
import java.nio.file.Paths

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    val userHome = System.getProperty("user.home")
    val outputPath = Paths.get(userHome, "/Develop/test_project").toString()

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