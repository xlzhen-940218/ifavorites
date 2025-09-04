package com.xlzhen.ifavorites.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

object MainFolderService {

    // 假设 RetrofitClient.instance.getMainFolders() 是一个 suspend 函数
    suspend fun getAndEnsureMainFolders(auth: String): List<Folder> {
        var response = RetrofitClient.instance.getMainFolders(auth)
        if (response.folders.isNullOrEmpty()) {
            RetrofitClient.instance.createMainFolders()
            response = RetrofitClient.instance.getMainFolders(auth)
        }
        return response.folders ?: emptyList()
    }

    // 封装成一个供 Java 调用的方法，返回 CompletableFuture
    @JvmStatic
    fun loadMainFoldersAsync(auth: String): CompletableFuture<List<Folder>> {
        val ioScope = CoroutineScope(Dispatchers.IO)
        return ioScope.future {
            // 在 IO 线程上执行协程代码
            getAndEnsureMainFolders(auth)
        }
    }
}