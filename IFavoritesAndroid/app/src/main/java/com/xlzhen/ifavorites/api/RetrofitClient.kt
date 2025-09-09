// File: app/src/main/java/com/example/cloudfavorites/RetrofitClient.kt
package com.xlzhen.ifavorites.api

import com.xlzhen.mvvm.storage.StorageUtils
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    //const val BASE_URL = "http://75.127.13.9:5000/"  // Replace with actual server URL
    val instance: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val httpClient = OkHttpClient.Builder().connectTimeout(120, TimeUnit.SECONDS) // 连接超时时间
            .readTimeout(120, TimeUnit.SECONDS)    // 读取超时时间
            .writeTimeout(120, TimeUnit.SECONDS)   // 写入超时时间
            .addInterceptor(logging).build()

        val retrofit = Retrofit.Builder()
            .baseUrl(ServerConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()

        retrofit.create(ApiService::class.java)
    }
}