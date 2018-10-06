package com.firefly.boot.cli

import com.firefly.boot.model.Project
import com.firefly.boot.service.impl.KotlinWebScaffoldServiceImpl
import com.firefly.utils.log.LogFactory
import com.firefly.utils.time.Millisecond100Clock
import java.nio.file.Paths

/**
 * @author Pengtao Qiu
 */
fun main(args: Array<String>) {
    try {
        val userHome = System.getProperty("user.home")
        val project = Project(
            "com.fireflysource.apple.tree", "apple-tree-website",
            "com.fireflysource.apple.tree",
            "apple.tree.fireflysource.com",
            Paths.get(userHome, "/Develop/test_project").toString()
        )
        val service = KotlinWebScaffoldServiceImpl()
        service.generate(project)
    } finally {
        LogFactory.getInstance().stop()
        Millisecond100Clock.stop()
    }
}