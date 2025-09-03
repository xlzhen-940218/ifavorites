# iFavorites

### 项目简介

iFavorites 是一个个人收藏夹管理系统，旨在帮助用户高效地收集、组织和管理自己的网络收藏夹。用户可以轻松添加、编辑、删除收藏链接，并通过自定义标签对它们进行分类，以便快速检索。

### 主要功能

  * **链接管理:** 添加、编辑和删除您的收藏链接。
  * **标签系统:** 为每个收藏链接添加自定义标签，实现灵活分类。
  * **搜索功能:** 强大的搜索功能，帮助您快速找到所需的收藏。
  * **简洁界面:** 干净、直观的用户界面，提供流畅的操作体验。
  * **视频下载:** 支持一键抓取 URL 并下载视频（需要安装 `yt-dlp` 库）。

### 技术栈

  * **后端:**
      * **Python:** 后端编程语言
      * **Flask:** 轻量级 Web 框架
      * **Requests:** 用于发送 HTTP 请求的库

### 快速开始

#### 先决条件

  * Python 3.6 或更高版本

#### 安装

1.  克隆项目仓库：

    ```
    git clone https://github.com/xlzhen-940218/ifavorites.git
    cd ifavorites
    ```

2.  创建并激活 Python 虚拟环境：

      * 对于 Windows:
        ```
        python -m venv venv
        .\venv\Scripts\activate
        ```
      * 对于 macOS 或 Linux:
        ```
        python3 -m venv venv
        source venv/bin/activate
        ```

3.  安装所需的依赖：

    ```
    pip install flask requests
    ```

#### 下载 yt-dlp 库

如果要使用一键视频下载功能，请运行以下脚本来安装并编译 `yt-dlp` 库：

  * 对于 Windows (PowerShell):
    ```
    .\download-ytdlp.ps1
    ```
  * 对于 macOS 或 Linux (Bash):
    ```
    ./download-ytdlp.sh
    ```

#### 运行项目

直接使用提供的脚本来启动服务器：

  * 对于 Windows (PowerShell):
    ```
    .\server.ps1
    ```
  * 对于 macOS 或 Linux (Bash):
    ```
    ./server.sh
    ```

服务器启动后，在浏览器中访问 `http://127.0.0.1:5000` 即可开始使用。

### 许可证

本项目采用 MIT 许可证，详情请参阅项目根目录下的 LICENSE 文件。

### 联系方式

如果您有任何问题或建议，可以通过 GitHub Issues 联系我。
