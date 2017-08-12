package com.firefly.kotlin.ext.example.task.management.service

import com.firefly.kotlin.ext.example.task.management.vo.ProjectEditor
import com.firefly.kotlin.ext.example.task.management.vo.ProjectResult
import com.firefly.kotlin.ext.example.task.management.vo.Request
import com.firefly.kotlin.ext.example.task.management.vo.Response

/**
 * @author Pengtao Qiu
 */
interface ProjectService {

    suspend fun createProject(request: Request<ProjectEditor>): Response<Long>

    suspend fun getProject(request: Request<Long>): Response<ProjectResult>

}