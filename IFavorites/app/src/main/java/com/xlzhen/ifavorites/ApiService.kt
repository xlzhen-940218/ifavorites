package com.xlzhen.ifavorites

import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    @POST("register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @Multipart
    @POST("upload_file")
    suspend fun uploadFile(@Header("Authorization") auth: String, @Part file: MultipartBody.Part): UploadResponse

    @Multipart
    @POST("upload_cover")
    suspend fun uploadCover(@Header("Authorization") auth: String, @Part file: MultipartBody.Part): UploadResponse

    @POST("create_main_folders")
    suspend fun createMainFolders(): GeneralResponse

    @GET("get_main_folders")
    suspend fun getMainFolders(@Header("Authorization") auth: String): FoldersResponse

    @POST("create_folder")
    suspend fun createFolder(@Header("Authorization") auth: String, @Body body: CreateFolderRequest): CreateFolderResponse

    @GET("get_sub_folders/{parent_id}")
    suspend fun getSubFolders(@Header("Authorization") auth: String, @Path("parent_id") parentId: String): FoldersResponse

    @POST("add_bookmark")
    suspend fun addBookmark(@Header("Authorization") auth: String, @Body body: AddBookmarkRequest): AddBookmarkResponse

    @GET("get_bookmarks/{folder_id}")
    suspend fun getBookmarks(@Header("Authorization") auth: String, @Path("folder_id") folderId: String): BookmarksResponse
}