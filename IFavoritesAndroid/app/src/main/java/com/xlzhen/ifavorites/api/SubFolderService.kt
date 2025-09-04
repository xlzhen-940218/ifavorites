package com.xlzhen.ifavorites.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

object SubFolderService {

    // 假设 RetrofitClient.instance.getMainFolders() 是一个 suspend 函数
    suspend fun getAndEnsureSubFolders(auth: String, mainFolderId: String): List<Folder> {
        val response = RetrofitClient.instance.getSubFolders(auth, mainFolderId)
        return response.folders ?: emptyList()
    }

    // 封装成一个供 Java 调用的方法，返回 CompletableFuture
    @JvmStatic
    fun loadSubFoldersAsync(auth: String, mainFolderId: String): CompletableFuture<List<Folder>> {
        val ioScope = CoroutineScope(Dispatchers.IO)
        return ioScope.future {
            // 在 IO 线程上执行协程代码
            getAndEnsureSubFolders(auth, mainFolderId)
        }
    }
}