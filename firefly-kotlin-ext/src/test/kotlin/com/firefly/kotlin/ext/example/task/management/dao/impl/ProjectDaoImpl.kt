package com.firefly.kotlin.ext.example.task.management.dao.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.kotlin.ext.db.*
import com.firefly.kotlin.ext.example.task.management.dao.ProjectDao
import com.firefly.kotlin.ext.example.task.management.model.Project
import com.firefly.kotlin.ext.example.task.management.model.ProjectUser

/**
 * @author Pengtao Qiu
 */
@Component
class ProjectDaoImpl : ProjectDao {

    @Inject
    lateinit var dbClient: AsyncHttpContextTransactionalManager

    override suspend fun insert(project: Project): Long? = dbClient.getConnection().execSQL {
        it.asyncInsertObject<Project, Long>(project)
    }

    override suspend fun addProjectMembers(projectId: Long, userIdList: List<Long>): List<Long> = dbClient.getConnection().execSQL {
        val projectUsers: List<ProjectUser> = userIdList.map { ProjectUser(0, it, projectId) }
        it.asyncInsertObjectBatch<ProjectUser, Long>(projectUsers)
    } ?: listOf()

    override suspend fun queryById(id: Long): Project? = dbClient.getConnection().execSQL {
        it.asyncQueryById<Project>(id)
    }

    override suspend fun listProjectMembers(projectId: Long): List<Long> = dbClient.getConnection().execSQL {
        it.asyncQuery<List<Long>>("select `user_id` from `test`.`project_user` where `project_id` = ?", {
            val ret = ArrayList<Long>()
            while (it.next()) {
                ret.add(it.getLong("user_id"))
            }
            ret
        }, projectId)
    } ?: listOf()

}