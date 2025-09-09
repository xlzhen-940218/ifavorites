package com.xlzhen.ifavorites.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

object CreateSubFolderService {

    // 假设 RetrofitClient.instance.getMainFolders() 是一个 suspend 函数
    suspend fun createAndEnsureSubFolders(auth: String, mainFolderId: String, name: String, userId: String): String? {
        val response = RetrofitClient.instance.createFolder(auth,CreateFolderRequest(name, mainFolderId, userId))
        return response.folder_id
    }

    // 封装成一个供 Java 调用的方法，返回 CompletableFuture
    @JvmStatic
    fun createSubFoldersAsync(auth: String, mainFolderId: String, name: String, userId: String): CompletableFuture<String?> {
        val ioScope = CoroutineScope(Dispatchers.IO)
        return ioScope.future {
            // 在 IO 线程上执行协程代码
            createAndEnsureSubFolders(auth, mainFolderId, name, userId)
        }
    }
}