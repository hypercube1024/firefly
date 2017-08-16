package com.firefly.kotlin.ext.example.task.management.service.impl

import com.firefly.annotation.Component
import com.firefly.annotation.Inject
import com.firefly.kotlin.ext.common.AsyncPool
import com.firefly.kotlin.ext.example.task.management.dao.ProjectDao
import com.firefly.kotlin.ext.example.task.management.dao.UserDao
import com.firefly.kotlin.ext.example.task.management.service.ProjectService
import com.firefly.kotlin.ext.example.task.management.vo.ProjectEditor
import com.firefly.kotlin.ext.example.task.management.vo.ProjectResult
import com.firefly.kotlin.ext.example.task.management.vo.Request
import com.firefly.kotlin.ext.example.task.management.vo.Response
import com.firefly.kotlin.ext.log.Log
import kotlinx.coroutines.experimental.async

/**
 * @author Pengtao Qiu
 */
@Component
class ProjectServiceImpl : ProjectService {

    private val log = Log.getLogger { }

    @Inject
    lateinit var projectDao: ProjectDao
    @Inject
    lateinit var userDao: UserDao

    override suspend fun createProject(request: Request<ProjectEditor>): Response<Long> {
        val projectId = projectDao.insert(request.data.project) ?: throw IllegalStateException("create project exception, the project id is null")
        log.info("create project -> $projectId")
        val ret = projectDao.addProjectMembers(projectId, request.data.userIdList)
        log.info("add members -> $ret")
        return Response(0, "success", projectId)
    }

    override suspend fun getProject(request: Request<Long>): Response<ProjectResult> {
        val projectDeferred = async(AsyncPool) { projectDao.queryById(request.data) }
        val userListDeferred = async(AsyncPool) {
            val users = projectDao.listProjectMembers(request.data)
            log.info("get project id ${request.data}, users -> $users")
            if (users.isEmpty()) listOf() else userDao.listUsers(users)
        }
        val project = projectDeferred.await() ?: return Response(404, "project not found", null)
        return Response(0, "success", ProjectResult(project, userListDeferred.await()))
    }

}