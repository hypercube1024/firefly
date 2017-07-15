package com.firefly.kotlin.ext.example.task.management.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.kotlin.ext.db.AsyncTransactionalJDBCHelper
import com.firefly.kotlin.ext.example.task.management.dao.ProjectDao
import com.firefly.kotlin.ext.example.task.management.model.Project
import com.firefly.kotlin.ext.example.task.management.model.ProjectUser

/**
 * @author Pengtao Qiu
 */
@Component
class ProjectDaoImpl : ProjectDao {

    @Inject
    lateinit var jdbcHelper: AsyncTransactionalJDBCHelper

    override suspend fun insert(project: Project): Long? = jdbcHelper.insertObject<Project, Long>(project)

    override suspend fun addProjectMembers(projectId: Long, userIdList: List<Long>): List<Long> {
        val projectUsers: List<ProjectUser> = userIdList.map { ProjectUser(0, it, projectId) }
        return jdbcHelper.insertObjectBatch<ProjectUser, Long>(projectUsers)
    }

    override suspend fun queryById(id: Long): Project? = jdbcHelper.queryById(id)

}