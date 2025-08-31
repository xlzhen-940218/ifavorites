package com.xlzhen.ifavorites

data class RegisterRequest(val email: String, val password: String)
data class RegisterResponse(val success: Boolean, val user_id: String?, val message: String?)

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val success: Boolean, val user_id: String?, val message: String?)

data class UploadResponse(val success: Boolean, val file_id: String?, val file_path: String?, val message: String?)

data class GeneralResponse(val success: Boolean, val message: String?)

data class Folder(val id: String, val name: String)
data class FoldersResponse(val success: Boolean, val folders: List<Folder>?, val message: String?)

data class CreateFolderRequest(val name: String, val parent_id: String, val user_id: String)
data class CreateFolderResponse(val success: Boolean, val folder_id: String?, val message: String?)

data class Bookmark(val id: String, val title: String, val description: String, val link: String, val cover: String?, val filepath: String?)
data class BookmarksResponse(val success: Boolean, val bookmarks: List<Bookmark>?, val message: String?)

data class AddBookmarkRequest(val title: String, val description: String, val folder_id: String, val link: String, val cover: String?, val user_id: String, val file_id: String?)
data class AddBookmarkResponse(val success: Boolean, val bookmark_id: String?, val message: String?)