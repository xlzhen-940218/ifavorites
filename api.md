# API 接口文档

## 认证

所有需要用户身份验证的接口都需要在请求头中添加 `Authorization`。
**请求头:**
`Authorization: Bearer <user_id>`
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

  * `message` (string): 错误信息（例如：“邮箱和密码是必填项”或“该邮箱已被注册”）。

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

  * `message` (string): 错误信息（例如：“邮箱或密码错误”）。

## 文件夹和收藏接口

### 3. 获取主文件夹列表

* **URL:** `/get_main_folders`

* **方法:** `GET`

* **描述:** 获取系统预设的主文件夹列表。如果数据库中不存在，会自动创建。

* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`

* **成功响应 (JSON):**

  * `success` (boolean): `true`

  * `folders` (array): 主文件夹对象列表。

    * `id` (string): 文件夹ID。

    * `name` (string): 文件夹名称。

### 4. 获取子文件夹列表

* **URL:** `/get_sub_folders/<parent_id>`

* **方法:** `GET`

* **描述:** 根据父文件夹ID获取其下的子文件夹列表。

* **URL参数:**

  * `parent_id` (string): 父文件夹的ID。

* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`

* **成功响应 (JSON):**

  * `success` (boolean): `true`

  * `folders` (array): 子文件夹对象列表。

    * `id` (string): 文件夹ID。

    * `name` (string): 文件夹名称。

### 5. 获取文件夹下的收藏列表

* **URL:** `/get_bookmarks/<folder_id>`

* **方法:** `GET`

* **描述:** 根据文件夹ID获取其下的所有收藏列表。

* **URL参数:**

  * `folder_id` (string): 文件夹的ID。

* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`

* **成功响应 (JSON):**

  * `success` (boolean): `true`

  * `bookmarks` (array): 收藏对象列表。

    * `title` (string): 收藏标题。

    * `description` (string): 收藏描述。

    * `link` (string): 收藏链接。

    * `cover` (string): 封面图片的文件路径。

    * `filepath` (string): 如果有文件关联，则为文件路径；否则为 `null`。

    * `id` (string): 收藏ID。

### 6. 创建子文件夹

* **URL:** `/create_folder`

* **方法:** `POST`

* **描述:** 在指定父文件夹下创建新的子文件夹。

* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`

* **请求体 (JSON):):**

  * `name` (string): 新文件夹的名称。

  * `parent_id` (string): 父文件夹的ID。

  * `user_id` (string): 用户ID。

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

  * `folder_id` (string): 要添加到的文件夹ID。

  * `link` (string): 收藏链接。

  * `cover` (string): 封面ID。

  * `user_id` (string): 用户ID。

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

* **失败响应 (JSON):**

  * `success` (boolean): `false`

  * `message` (string): 错误信息。

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

* **失败响应 (JSON):**

  * `success` (boolean): `false`

  * `message` (string): 错误信息。

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

### 12. 爬取URL并保存为收藏

* **URL:** `/craw_url`

* **方法:** `POST`

* **描述:** 爬取指定的URL（例如：YouTube视频），下载视频和封面，并将其作为收藏项保存。

* **请求头:** 需要提供有效的 `Authorization: Bearer <user_id>`

* **请求体 (JSON):**

  * `link` (string): 要爬取的URL。

  * `folder_id` (string): 目标文件夹ID。

* **成功响应 (JSON):**

  * `success` (boolean): `true`

  * `bookmark_id` (string): 新创建的收藏ID。

* **失败响应 (JSON):**

  * `success` (boolean): `false`

  * `message` (string): 错误信息。

## 其他接口

### 13. 初始化主文件夹（内部调用）

* **URL:** `/create_main_folders`

* **方法:** `POST`

* **描述:** 创建预设的主文件夹类型。该接口通常用于系统初始化，用户无需直接调用。

* **成功响应 (JSON):**

  * `success` (boolean): `true`

  * `message` (string): 成功或已存在的信息。

### 14. 首页和索引页

* **URL:** `/`

* **方法:** `GET`

* **描述:** 渲染 `complete.html` 页面。

* **URL:** `/index`

* **方法:** `GET`

* **描述:** 渲染 `index.html` 页面。
