package com.firefly.kotlin.ext.example.task.management.dao

import com.firefly.kotlin.ext.example.task.management.model.Project

/**
 * @author Pengtao Qiu
 */
interface ProjectDao {

    suspend fun insert(project: Project): Long?

    suspend fun addProjectMembers(projectId: Long, userIdList: List<Long>): List<Long>

    suspend fun queryById(id: Long): Project?

    suspend fun listProjectMembers(projectId: Long): List<Long>

}