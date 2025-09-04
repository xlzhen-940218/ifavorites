package com.xlzhen.ifavorites.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

object BookmarkService {

    // 假设 RetrofitClient.instance.getMainFolders() 是一个 suspend 函数
    suspend fun getAndEnsureBookmark(auth: String, folderId: String): List<Bookmark> {
        val response = RetrofitClient.instance.getBookmarks(auth, folderId)
        return response.bookmarks ?: emptyList()
    }

    // 封装成一个供 Java 调用的方法，返回 CompletableFuture
    @JvmStatic
    fun loadBookmarkAsync(auth: String, folderId: String): CompletableFuture<List<Bookmark>> {
        val ioScope = CoroutineScope(Dispatchers.IO)
        return ioScope.future {
            // 在 IO 线程上执行协程代码
            getAndEnsureBookmark(auth, folderId)
        }
    }
}