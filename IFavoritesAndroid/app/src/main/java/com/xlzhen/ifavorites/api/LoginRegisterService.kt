package com.xlzhen.ifavorites.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

object LoginRegisterService {

    // 假设 RetrofitClient.instance.getMainFolders() 是一个 suspend 函数
    suspend fun loginRegister(email: String, password: String, loginOrRegister: Boolean): String? {
        val user_id = if (!loginOrRegister) RetrofitClient.instance.register(
            RegisterRequest(
                email,
                password
            )
        ).user_id
        else RetrofitClient.instance.login(LoginRequest(email, password)).user_id

        return user_id
    }

    // 封装成一个供 Java 调用的方法，返回 CompletableFuture
    @JvmStatic
    fun loginRegisterAsync(email: String, password: String, loginOrRegister: Boolean): CompletableFuture<String?> {
        val ioScope = CoroutineScope(Dispatchers.IO)
        return ioScope.future {
            // 在 IO 线程上执行协程代码
            loginRegister(email, password, loginOrRegister)
        }
    }
}