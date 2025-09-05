package com.xlzhen.ifavorites.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

object RecoveryTasksUrlService {

    // 假设 RetrofitClient.instance.getMainFolders() 是一个 suspend 函数
    suspend fun recoveryAndEnsureTasksUrl(auth: String, folderId: String): List<String> {
        val response = RetrofitClient.instance.recoveryTasksUrl(auth, RecoveryTasksUrlRequest( folderId))
        return if (!response.task_ids.isNullOrEmpty()) {
            response.task_ids
        } else {
            emptyList()
        }
    }

    // 封装成一个供 Java 调用的方法，返回 CompletableFuture
    @JvmStatic
    fun recoveryTasksUrlAsync(auth: String, folderId: String): CompletableFuture<List<String>> {
        val ioScope = CoroutineScope(Dispatchers.IO)
        return ioScope.future {
            // 在 IO 线程上执行协程代码
            recoveryAndEnsureTasksUrl(auth,folderId)
        }
    }
}