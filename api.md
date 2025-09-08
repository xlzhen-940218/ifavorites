# **书签管理系统 API 文档**

## **概述**

本文档提供了书签管理系统后端服务的 API 接口说明。所有 API 请求和响应的主体格式均为 JSON。

### **认证**

对于需要认证的接口，客户端必须在 HTTP 请求头中包含 `Authorization` 字段。

  - **格式**: `Authorization: Bearer <user_id>`
  - **示例**: `Authorization: Bearer 123e4567-e89b-12d3-a456-426614174000`

如果认证失败或未提供令牌，服务器将返回 `401 Unauthorized` 错误。

### **通用响应格式**

  - **成功响应**:
    ```json
    {
      "success": true,
      "data": { ... } // 或其他特定字段
    }
    ```
  - **失败响应**:
    ```json
    {
      "success": false,
      "message": "错误信息描述"
    }
    ```

-----

## **1. 用户认证 (User Authentication)**

### **1.1 用户注册**

  - **Endpoint**: `/register`
  - **Method**: `POST`
  - **Description**: 创建一个新用户账户。
  - **Authentication**: 无需

**Request Body**:

```json
{
  "email": "user@example.com",
  "password": "your_secure_password"
}
```

| 参数       | 类型   | 描述       | 是否必须 |
| :--------- | :----- | :--------- | :------- |
| `email`    | String | 用户邮箱   | 是       |
| `password` | String | 用户密码   | 是       |

**Success Response (`201 Created`)**:

```json
{
  "success": true,
  "user_id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
}
```

**Error Responses**:

  - `400 Bad Request`: 缺少 `email` 或 `password`。
  - `409 Conflict`: 该邮箱已被注册。

-----

### **1.2 用户登录**

  - **Endpoint**: `/login`
  - **Method**: `POST`
  - **Description**: 验证用户凭据并返回用户ID（作为后续请求的令牌）。
  - **Authentication**: 无需

**Request Body**:

```json
{
  "email": "user@example.com",
  "password": "your_secure_password"
}
```

**Success Response (`200 OK`)**:

```json
{
  "success": true,
  "user_id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
}
```

**Error Responses**:

  - `400 Bad Request`: 缺少 `email` 或 `password`。
  - `401 Unauthorized`: 邮箱或密码错误。

-----

## **2. 数据查询 (Data Retrieval)**

### **2.1 获取主文件夹**

  - **Endpoint**: `/get_main_folders`
  - **Method**: `GET`
  - **Description**: 获取所有顶级的分类文件夹。如果数据库为空，会自动创建默认分类。
  - **Authentication**: **需要**

**Success Response (`200 OK`)**:

```json
{
  "success": true,
  "folders": [
    {
      "id": "main_folder_id_1",
      "name": "视频"
    },
    {
      "id": "main_folder_id_2",
      "name": "音频"
    }
  ]
}
```

-----

### **2.2 获取子文件夹**

  - **Endpoint**: `/get_sub_folders/<parent_id>`
  - **Method**: `GET`
  - **Description**: 获取指定主文件夹下的所有子文件夹。
  - **Authentication**: **需要**

**URL Parameters**:
| 参数        | 类型   | 描述             |
| :---------- | :----- | :--------------- |
| `parent_id` | String | 主文件夹的 ID    |

**Success Response (`200 OK`)**:

```json
{
  "success": true,
  "folders": [
    {
      "id": "sub_folder_id_1",
      "name": "学习资料"
    },
    {
      "id": "sub_folder_id_2",
      "name": "娱乐"
    }
  ]
}
```

-----

### **2.3 获取文件夹内的书签**

  - **Endpoint**: `/get_bookmarks/<folder_id>`
  - **Method**: `GET`
  - **Description**: 获取指定文件夹下的所有书签列表。
  - **Authentication**: **需要**

**URL Parameters**:
| 参数        | 类型   | 描述                     |
| :---------- | :----- | :----------------------- |
| `folder_id` | String | 文件夹的 ID (主或子)   |

**Success Response (`200 OK`)**:

```json
{
  "success": true,
  "bookmarks": [
    {
      "id": "bookmark_id_1",
      "title": "视频标题",
      "description": "视频描述...",
      "link": "https://www.youtube.com/watch?v=...",
      "cover": "covers/cover_file_id.jpg",
      "filepath": "files/video_file_id.mp4"
    }
  ]
}
```

*注：`cover` 和 `filepath` 是相对路径，需要与服务器域名拼接成完整 URL。如果书签没有关联文件，值为 `null`。*

-----

## **3. 数据创建与修改 (Data Creation & Modification)**

### **3.1 创建子文件夹**

  - **Endpoint**: `/create_folder`
  - **Method**: `POST`
  - **Description**: 在一个主文件夹下创建一个新的子文件夹。
  - **Authentication**: **需要**

**Request Body**:

```json
{
  "name": "我的技术收藏",
  "parent_id": "main_folder_id_for_documents"
}
```

| 参数        | 类型   | 描述               | 是否必须 |
| :---------- | :----- | :----------------- | :------- |
| `name`      | String | 新文件夹的名称     | 是       |
| `parent_id` | String | 所属主文件夹的 ID  | 是       |

**Success Response (`201 Created`)**:

```json
{
  "success": true,
  "folder_id": "newly_created_folder_id"
}
```

-----

### **3.2 手动添加书签**

  - **Endpoint**: `/add_bookmark`
  - **Method**: `POST`
  - **Description**: 手动添加一个书签条目，通常与手动上传封面和文件配合使用。
  - **Authentication**: **需要**

**Request Body**:

```json
{
  "title": "手动添加的标题",
  "description": "这是手动添加的书签描述",
  "folder_id": "target_folder_id",
  "link": "https://example.com/some_article",
  "cover": "uploaded_cover_file_id",
  "file_id": "uploaded_file_id" 
}
```

| 参数          | 类型   | 描述                                     | 是否必须 |
| :------------ | :----- | :--------------------------------------- | :------- |
| `title`       | String | 书签标题                                 | 是       |
| `description` | String | 书签描述                                 | 是       |
| `folder_id`   | String | 要存入的文件夹 ID                        | 是       |
| `link`        | String | 原始链接                                 | 是       |
| `cover`       | String | 封面 ID (通过 `/upload_cover` 接口获取) | 是       |
| `file_id`     | String | 文件 ID (通过 `/upload_file` 接口获取)  | 否       |

**Success Response (`201 Created`)**:

```json
{
  "success": true,
  "bookmark_id": "newly_created_bookmark_id"
}
```

-----

## **4. 文件上传与访问 (File Uploads & Access)**

### **4.1 上传文件**

  - **Endpoint**: `/upload_file`
  - **Method**: `POST`
  - **Description**: 上传一个文件（如视频、文档），返回文件 ID 和路径。
  - **Authentication**: **需要**
  - **Request Body**: `multipart/form-data`
      - `file`: 要上传的文件本身。

**Success Response (`200 OK`)**:

```json
{
  "success": true,
  "file_id": "new_file_id",
  "file_path": "files/new_file_id.mp4"
}
```

### **4.2 上传封面**

  - **Endpoint**: `/upload_cover`
  - **Method**: `POST`
  - **Description**: 上传一个封面图片，返回封面 ID 和路径。
  - **Authentication**: **需要**
  - **Request Body**: `multipart/form-data`
      - `file`: 要上传的图片文件。

**Success Response (`200 OK`)**:

```json
{
  "success": true,
  "file_id": "new_cover_id",
  "file_path": "covers/new_cover_id.jpg"
}
```

### **4.3 访问文件/封面**

  - **Endpoint**: `/files/<filename>` 或 `/covers/<filename>`
  - **Method**: `GET`
  - **Description**: 通过文件名直接访问已上传的文件或封面。这些 URL 通常由其他 API (如 `/get_bookmarks`) 返回。

-----

## **5. 后台任务管理 (Background Task Management)**

### **5.1 提交下载任务**

  - **Endpoint**: `/craw_url`
  - **Method**: `POST`
  - **Description**: 提交一个 URL 进行后台抓取和下载。此接口能自动识别单个视频 URL 和 YouTube 播放列表 URL。
  - **Authentication**: **需要**

**Request Body**:

```json
{
  "link": "https://www.youtube.com/watch?v=some_video_id",
  "folder_id": "target_folder_id",
  "is_download": true
}
```

| 参数            | 类型      | 描述                | 是否必须     |
|:--------------|:--------|:------------------|:---------|
| `link`        | String  | 要下载的视频或播放列表的 URL  | 是        |
| `folder_id`   | String  | 下载完成后书签要存入的文件夹 ID | 是        |
| `is_download` | Boolean | 是否下载视频            | 否（默认不下载） |

**Success Response (`200 OK`)**:

  - **单个视频**:
    ```json
    {
      "success": true,
      "message": "任务已成功提交",
      "task_id": "single_task_id"
    }
    ```
  - **播放列表**:
    ```json
    {
      "success": true,
      "message": "N个任务已成功提交",
      "task_ids": [
        "task_id_1",
        "task_id_2",
        ...
      ]
    }
    ```

-----

### **5.2 获取任务进度**

  - **Endpoint**: `/get_progress/<task_id>`
  - **Method**: `GET`
  - **Description**: 查询指定后台任务的当前状态和进度。
  - **Authentication**: **需要**

**URL Parameters**:
| 参数      | 类型   | 描述           |
| :-------- | :----- | :------------- |
| `task_id` | String | 任务的 ID      |

**Success Response (`200 OK`)**:

```json
{
  "success": true,
  "task_id": "queried_task_id",
  "status": "DOWNLOADING", // PENDING, DOWNLOADING, COMPLETED, FAILED
  "progress": 50, // 进度百分比 (0-100)
  "message": "正在下载视频..."
}
```

**Error Responses**:

  - `404 Not Found`: 任务不存在或用户无权查看。

-----

### **5.3 查询未完成的任务**

  - **Endpoint**: `/recover_tasks`
  - **Method**: `POST`
  - **Description**: 查询指定文件夹下所有未完成（非 `COMPLETED` 状态）的任务 ID 列表。此接口仅用于查询，真正的任务恢复在服务启动时自动进行。
  - **Authentication**: **需要**

**Request Body**:

```json
{
  "folder_id": "target_folder_id"
}
```

**Success Response (`200 OK`)**:

```json
{
  "success": true,
  "message": "找到 2 个未完成的任务。",
  "task_ids": [
    "unfinished_task_id_1",
    "unfinished_task_id_2"
  ]
}
```
