# API 接口文档

## 认证

所有需要用户身份验证的接口都需要在请求头中添加 **`Authorization`**。

**请求头:** `Authorization: Bearer <user_id>`

其中 `<user_id>` 是用户登录或注册后获取到的唯一用户ID。

## 用户接口

### 1. 用户注册

* **URL:** `/register`
* **方法:** `POST`
* **描述:** 新用户注册。
* **请求体 (JSON):**
    * `email` (string): 用户邮箱，必须唯一。
    * `password` (string): 用户密码。
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `user_id` (string): 新注册用户的ID。
* **失败响应 (JSON):**
    * `success` (boolean): `false`
    * `message` (string): 错误信息。

### 2. 用户登录

* **URL:** `/login`
* **方法:** `POST`
* **描述:** 用户登录。
* **请求体 (JSON):**
    * `email` (string): 用户邮箱。
    * `password` (string): 用户密码。
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `user_id` (string): 登录用户的ID。
* **失败响应 (JSON):**
    * `success` (boolean): `false`
    * `message` (string): 错误信息。

## 文件夹和收藏接口

### 3. 获取主文件夹列表

* **URL:** `/get_main_folders`
* **方法:** `GET`
* **描述:** 获取系统预设的主文件夹列表。
* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `folders` (array): 主文件夹对象列表。

### 4. 获取子文件夹列表

* **URL:** `/get_sub_folders/<parent_id>`
* **方法:** `GET`
* **描述:** 根据父文件夹ID获取子文件夹列表。
* **URL参数:**
    * `parent_id` (string): 父文件夹的ID。
* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `folders` (array): 子文件夹对象列表。

### 5. 获取文件夹下的收藏列表

* **URL:** `/get_bookmarks/<folder_id>`
* **方法:** `GET`
* **描述:** 根据文件夹ID获取所有收藏。
* **URL参数:**
    * `folder_id` (string): 文件夹的ID。
* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `bookmarks` (array): 收藏对象列表。

### 6. 创建子文件夹

* **URL:** `/create_folder`
* **方法:** `POST`
* **描述:** 在指定父文件夹下创建新的子文件夹。
* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`
* **请求体 (JSON):**
    * `name` (string): 新文件夹的名称。
    * `parent_id` (string): 父文件夹的ID。
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `folder_id` (string): 新创建的文件夹ID。

### 7. 添加收藏

* **URL:** `/add_bookmark`
* **方法:** `POST`
* **描述:** 向指定文件夹添加一个新收藏。
* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`
* **请求体 (JSON):**
    * `title` (string): 收藏标题。
    * `description` (string): 收藏描述。
    * `folder_id` (string): 目标文件夹ID。
    * `link` (string): 收藏链接。
    * `cover` (string): 封面ID。
    * `file_id` (string, 可选): 如果与文件关联，则为文件ID。
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `bookmark_id` (string): 新添加的收藏ID。

## 文件和内容接口

### 8. 上传文件

* **URL:** `/upload_file`
* **方法:** `POST`
* **描述:** 上传文件并将其信息保存到数据库。
* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`
* **请求体 (Form Data):**
    * `file` (file): 要上传的文件。
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `file_id` (string): 上传文件的ID。
    * `file_path` (string): 上传文件的服务器路径。

### 9. 上传封面

* **URL:** `/upload_cover`
* **方法:** `POST`
* **描述:** 上传封面图片并将其信息保存到数据库。
* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`
* **请求体 (Form Data):**
    * `file` (file): 要上传的封面图片。
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `file_id` (string): 上传封面的ID。
    * `file_path` (string): 上传封面的服务器路径。

### 10. 获取封面文件

* **URL:** `/covers/<filename>`
* **方法:** `GET`
* **描述:** 根据文件名获取存储在服务器上的封面图片文件。
* **URL参数:**
    * `filename` (string): 封面图片的文件名。

### 11. 获取普通文件

* **URL:** `/files/<filename>`
* **方法:** `GET`
* **描述:** 根据文件名获取存储在服务器上的文件。
* **URL参数:**
    * `filename` (string): 文件名。

### 12. 提交异步爬取任务

* **URL:** `/craw_url`
* **方法:** `POST`
* **描述:** 提交一个异步任务，用于爬取指定的URL并将其作为收藏项保存。
* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`
* **请求体 (JSON):**
    * `link` (string): 要爬取的URL。
    * `folder_id` (string): 目标文件夹ID。
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `task_id` (string): 新创建的任务ID。

### 13. 查询任务进度

* **URL:** `/get_progress/<task_id>`
* **方法:** `GET`
* **描述:** 根据任务ID获取任务的当前状态和进度。
* **URL参数:**
    * `task_id` (string): 任务的唯一ID。
* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `status` (string): 任务状态，例如: `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`。
    * `progress` (integer): 任务进度百分比。
    * `message` (string): 任务的详细状态信息。

## 其他接口

### 14. 初始化主文件夹

* **URL:** `/create_main_folders`
* **方法:** `POST`
* **描述:** 创建预设的主文件夹类型。
* **成功响应 (JSON):**
    * `success` (boolean): `true`
    * `message` (string): 成功或已存在的信息。

### 15. 首页和索引页

* **URL:** `/`
* **方法:** `GET`
* **描述:** 渲染 `complete.html` 页面。
* **URL:** `/index`
* **方法:** `GET`
* **描述:** 渲染 `index.html` 页面。
