package com.firefly.kotlin.ext.example.task.management.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.kotlin.ext.db.*
import com.firefly.kotlin.ext.example.task.management.dao.ProjectDao
import com.firefly.kotlin.ext.example.task.management.model.Project
import com.firefly.kotlin.ext.example.task.management.model.ProjectUser
import java.util.stream.Collectors

/**
 * @author Pengtao Qiu
 */
@Component
class ProjectDaoImpl : ProjectDao {

    @Inject
    lateinit var dbClient: AsyncTransactionalManager

    override suspend fun insert(project: Project): Long? = dbClient.execSQL {
        it.asyncInsertObject<Project, Long>(project)
    }

    override suspend fun addProjectMembers(projectId: Long, userIdList: List<Long>): List<Long> = dbClient.execSQL {
        val projectUsers: List<ProjectUser> = userIdList.map { ProjectUser(0, it, projectId) }
        it.asyncInsertObjectBatch<ProjectUser, Long>(projectUsers)
    }

    override suspend fun queryById(id: Long): Project? = dbClient.execSQL {
        it.asyncQueryById<Project>(id)
    }

    override suspend fun listProjectMembers(projectId: Long): List<Long> = dbClient.execSQL {
        it.asyncQuery<List<Long>>("select `user_id` from `test`.`project_user` where `project_id` = ?", {
            it.stream().map { it.getLong("user_id") }.collect(Collectors.toList())
        }, projectId)
    }

}