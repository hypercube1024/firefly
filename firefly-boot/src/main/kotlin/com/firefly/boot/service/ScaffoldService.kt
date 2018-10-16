package com.firefly.boot.service

import com.firefly.boot.model.Project

/**
 * @author Pengtao Qiu
 */
interface ScaffoldService {
    fun generate(project: Project)
}