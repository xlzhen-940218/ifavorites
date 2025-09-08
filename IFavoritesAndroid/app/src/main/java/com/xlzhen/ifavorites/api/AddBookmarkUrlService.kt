package com.xlzhen.ifavorites.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

object AddBookmarkUrlService {

    // 假设 RetrofitClient.instance.getMainFolders() 是一个 suspend 函数
    suspend fun addAndEnsureBookmarkUrl(
        auth: String,
        folderId: String,
        link: String,
        isDownload: Boolean
    ): List<String> {
        val response = RetrofitClient.instance.addBookmarkUrl(
            auth,
            AddBookmarkUrlRequest(folderId, link, isDownload)
        )
        return if (!response.task_ids.isNullOrEmpty()) {
            response.task_ids
        } else if (response.task_id != null) {
            listOf(response.task_id)
        } else {
            emptyList()
        }
    }

    // 封装成一个供 Java 调用的方法，返回 CompletableFuture
    @JvmStatic
    fun addBookmarkUrlAsync(
        auth: String,
        folderId: String,
        link: String,
        isDownload: Boolean
    ): CompletableFuture<List<String>> {
        val ioScope = CoroutineScope(Dispatchers.IO)
        return ioScope.future {
            // 在 IO 线程上执行协程代码
            addAndEnsureBookmarkUrl(auth, folderId, link, isDownload)
        }
    }
}