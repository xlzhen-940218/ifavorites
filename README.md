# iFavorites

> 个人收藏夹管理系统，高效组织、管理和检索您的网络收藏。

## ✨ 项目亮点

  - **智能收藏**: 支持一键抓取网页元数据，并自动下载视频、封面（基于 `yt-dlp`）。
  - **灵活组织**: 通过自定义标签和多级文件夹系统，对收藏进行高效分类和管理。
  - **快速检索**: 强大的全局搜索功能，帮助您在海量收藏中迅速找到所需内容。
  - **简洁直观**: 设计简洁的用户界面，提供流畅、愉悦的操作体验。
  - **模块化后端**: 基于 Python Flask 的轻量级后端，易于扩展和维护。

## 🚀 快速开始

### 🛠️ 先决条件

  - **Python 3.6+**

### 📦 安装与配置

1.  **克隆仓库**

    ```bash
    git clone https://github.com/xlzhen-940218/ifavorites.git
    cd ifavorites
    ```

2.  **创建并激活虚拟环境**

      - **Windows**
        ```bash
        python -m venv venv
        .\venv\Scripts\activate
        ```
      - **macOS / Linux**
        ```bash
        python3 -m venv venv
        source venv/bin/activate
        ```

3.  **安装依赖**

    ```bash
    pip install -r requirements.txt
    ```

    > **注意**：如果项目根目录没有 `requirements.txt` 文件，请运行 `pip install flask requests`。

4.  **下载 `yt-dlp`**
    为了启用视频下载功能，请运行以下脚本：

      - **Windows** (PowerShell)
        ```bash
        .\download-ytdlp.ps1
        ```
      - **macOS / Linux** (Bash)
        ```bash
        ./download-ytdlp.sh
        ```

### 🏃‍♂️ 运行项目

直接使用提供的脚本来启动服务器：

  - **Windows** (PowerShell)
    ```bash
    .\server.ps1
    ```
  - **macOS / Linux** (Bash)
    ```bash
    ./server.sh
    ```

服务器启动后，在浏览器中打开 `http://127.0.0.1:5000` 即可开始使用。

## 许可协议

本项目采用 **MIT 许可证**，详情请参阅项目根目录下的 [LICENSE](https://www.google.com/search?q=LICENSE) 文件。

## 联系我们

如果您有任何问题、建议或发现了 bug，请随时通过 **GitHub Issues** 与我们联系。
