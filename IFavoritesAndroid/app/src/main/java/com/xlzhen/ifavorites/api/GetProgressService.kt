package com.xlzhen.ifavorites.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

object GetProgressService {

    /**
     * Kotlin 协程方法，用于获取单个任务的进度。
     * @param auth 用户认证 token。
     * @param taskId 任务的唯一 ID。
     * @return GetProgressResponse 类型的响应对象，如果请求失败则返回 null。
     */
    suspend fun getProgress(auth: String, taskId: String): Progress? {
        val response = RetrofitClient.instance.getProgress(auth, taskId)
        return Progress(response.success,response.status,response.progress,response.message)
    }

    /**
     * 封装为供 Java 调用的异步方法。
     * @param auth 用户认证 token。
     * @param taskId 任务的唯一 ID。
     * @return CompletableFuture<GetProgressResponse?> 异步结果。
     */
    @JvmStatic
    fun getProgressAsync(auth: String, taskId: String): CompletableFuture<Progress?> {
        val ioScope = CoroutineScope(Dispatchers.IO)
        return ioScope.future {
            // 在 IO 线程上执行协程代码
            getProgress(auth, taskId)
        }
    }
}