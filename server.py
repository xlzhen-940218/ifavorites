# -*- coding: utf-8 -*-

# --------------------------------------------------------------------------- #
# 导入标准库和第三方库
# --------------------------------------------------------------------------- #
import json
import locale
import os
import sqlite3
import subprocess
import sys
import uuid
import threading
from functools import wraps
from time import sleep

import requests
from flask import Flask, request, jsonify, render_template, send_from_directory, g, Response
from werkzeug.utils import secure_filename

# --------------------------------------------------------------------------- #
# 全局配置和初始化
# --------------------------------------------------------------------------- #
app = Flask(__name__)

# --- 数据库配置 ---
DB_NAME = 'bookmarks.db'

# --- 文件上传路径配置 ---
UPLOAD_FOLDER = 'files'  # 普通文件（如视频）的存储目录
UPLOAD_COVER = 'covers'  # 封面图片的存储目录
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['UPLOAD_COVER'] = UPLOAD_COVER

# --- 外部工具路径配置 ---
# 根据操作系统平台确定 yt-dlp 可执行文件的路径
if sys.platform == "win32":
    YTDLP_CMD = "yt-dlp.exe"
else:
    YTDLP_CMD = "./yt-dlp-exec"  # 假设在 Linux/macOS 下，可执行文件位于同级目录

# --- 并发控制 ---
# 创建一个线程信号量，限制同时进行的下载任务最多为3个，防止服务器过载
MAX_CONCURRENT_DOWNLOADS = 6
work_task_ids = []
download_semaphore = threading.Semaphore(MAX_CONCURRENT_DOWNLOADS)

# --- 确保上传目录存在 ---
os.makedirs(UPLOAD_FOLDER, exist_ok=True)
os.makedirs(UPLOAD_COVER, exist_ok=True)

# 定义一个字典，键是语言代码，值是对应的翻译列表
TRANSLATIONS = {
    'zh': ["视频", "音频", "图片", "文档", "压缩文件", "其他"],
    'en': ["Videos", "Audios", "Photos", "Documents", "Compressed", "Others"],
    'fr': ["Vidéos", "Audios", "Photos", "Documents", "Compressés", "Autres"],
    'de': ["Videos", "Audios", "Fotos", "Dokumente", "Komprimierte", "Andere"],
    'es': ["Videos", "Audios", "Fotos", "Documentos", "Comprimidos", "Otros"],
    'it': ["Video", "Audio", "Foto", "Documenti", "Compressi", "Altri"],
    'ja': ["動画", "音声", "画像", "ドキュメント", "圧縮ファイル", "その他"],
    'ko': ["동영상", "오디오", "사진", "문서", "압축 파일", "기타"],
}

# 获取系统语言，例如 'zh_CN', 'en_US'
lang_code = locale.getdefaultlocale()[0]

# 提取语言前缀，例如从 'zh_CN' 中提取 'zh'
if lang_code:
    language = lang_code.split('_')[0]
else:
    # 如果无法获取，默认使用英语
    language = 'en'


# --------------------------------------------------------------------------- #
# 数据库辅助函数
# --------------------------------------------------------------------------- #
def get_db_connection():
    """
    获取数据库连接。
    使用 Flask 的 g 对象，确保在同一次请求中复用同一个数据库连接。
    """
    if 'db' not in g:
        g.db = sqlite3.connect(DB_NAME)
        g.db.row_factory = sqlite3.Row  # 设置 row_factory 以便将查询结果作为字典访问
    return g.db


@app.teardown_appcontext
def close_db_connection(exception):
    """
    在应用上下文结束时（通常是请求结束后）自动关闭数据库连接。
    """
    db = g.pop('db', None)
    if db is not None:
        db.close()


def init_db():
    """
    初始化数据库：创建所有需要的表结构。
    这个函数应该在应用首次启动前运行。
    """
    with sqlite3.connect(DB_NAME) as conn:
        cursor = conn.cursor()
        # 用户表: 存储用户信息
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
        ''')
        # 文件表: 存储上传的视频等文件信息
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS files (
                id TEXT PRIMARY KEY,
                original_filename TEXT NOT NULL,
                filepath TEXT NOT NULL
            )
        ''')
        # 封面表: 存储上传或下载的封面图片信息
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS covers (
                id TEXT PRIMARY KEY,
                original_filename TEXT NOT NULL,
                filepath TEXT NOT NULL
            )
        ''')
        # 文件夹表: 存储用户创建的子文件夹，通过 parent_id 形成层级结构
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS folders (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                parent_id TEXT,
                FOREIGN KEY (parent_id) REFERENCES folders(id)
            )
        ''')
        # 收藏表: 存储核心的书签信息
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS bookmarks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT,
                link TEXT UNIQUE NOT NULL,
                cover TEXT, -- 封面ID，外键关联 covers 表
                file_id TEXT  -- 文件ID，外键关联 files 表
            )
        ''')
        # 主文件夹表: 存储预设的顶级分类
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS main_folders (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL UNIQUE
            )
        ''')
        # 用户-文件夹关系表: 关联用户和他们有权访问的文件夹
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS user_folders (
                user_id TEXT,
                folder_id TEXT,
                PRIMARY KEY (user_id, folder_id),
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (folder_id) REFERENCES folders(id)
            )
        ''')
        # 用户-文件夹-收藏关系表: 将一个收藏条目放入特定用户的特定文件夹中
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS user_folder_bookmarks (
                user_id TEXT,
                folder_id TEXT,
                bookmark_id TEXT,
                PRIMARY KEY (user_id, folder_id, bookmark_id),
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (folder_id) REFERENCES folders(id),
                FOREIGN KEY (bookmark_id) REFERENCES bookmarks(id)
            )
        ''')
        # 任务表: 用于跟踪后台下载任务的状态
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                link TEXT NOT NULL,
                user_id TEXT NOT NULL,
                folder_id TEXT NOT NULL,
                status TEXT NOT NULL, -- PENDING, DOWNLOADING, COMPLETED, FAILED
                progress INTEGER,
                is_download INTEGER,
                message TEXT,
                result TEXT, -- 成功时可存储 bookmark_id
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        conn.commit()
        app.logger.info("数据库初始化完成。")


def check_duplicate_bookmark(link):
    """
    通过链接检查数据库中是否已存在相同的书签。
    :param link: 要检查的书签链接。
    :return: 如果存在，返回书签ID；否则返回 None。
    """
    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT id FROM bookmarks WHERE link = ?", (link,))
    result = cursor.fetchone()
    return result[0] if result else None


# --------------------------------------------------------------------------- #
# 认证与授权辅助函数/装饰器
# --------------------------------------------------------------------------- #
def login_required(f):
    """
    一个装饰器，用于保护需要登录才能访问的路由。
    它会检查请求头中的 'Authorization: Bearer <user_id>'。
    如果验证成功，会将 user_id 作为参数传递给被装饰的函数。
    如果失败，则返回 401 未授权错误。
    """

    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({"success": False, "message": "未提供授权令牌"}), 401

        user_id = auth_header.split(' ')[1]
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM users WHERE id = ?", (user_id,))
        if not cursor.fetchone():
            return jsonify({"success": False, "message": "无效的用户ID"}), 401

        # 将 user_id 传递给路由函数
        return f(user_id=user_id, *args, **kwargs)

    return decorated_function


# --------------------------------------------------------------------------- #
# 核心业务逻辑 (后台任务)
# --------------------------------------------------------------------------- #
def process_video_download(task_id, link, folder_id, user_id, is_download):
    with app.app_context():
        if task_id not in work_task_ids:
            work_task_ids.append(task_id)
        else:
            app.logger.error("已经在任务列表。不可重复添加！")
            return
        """
        在后台线程中处理视频下载的完整流程。
        包括：获取元数据、下载封面、下载视频、保存信息到数据库，并实时更新任务状态。
        """
        # 使用 with 语句确保信号量在任何情况下都会被释放
        with download_semaphore:
            try:
                # --- 内部辅助函数：用于更新数据库中的任务状态 ---
                def update_task_status(status, progress=None, message=None, result=None):
                    app.logger.info(f"[任务更新] ID: {task_id}, 状态: {status}, 进度: {progress}, 消息: {message}")
                    with sqlite3.connect(DB_NAME) as conn:
                        cursor = conn.cursor()
                        if progress is not None:
                            cursor.execute(
                                "UPDATE tasks SET status = ?, progress = ?, message = ?, result = ? WHERE id = ?",
                                (status, progress, message, result, task_id)
                            )
                        else:
                            cursor.execute(
                                "UPDATE tasks SET status = ?, message = ?, result = ? WHERE id = ?",
                                (status, message, result, task_id)
                            )
                        conn.commit()

                # 1. 更新任务状态为“下载中”
                update_task_status('DOWNLOADING', 0, '任务已开始...')

                # 2. 使用 yt-dlp 获取视频元数据 (标题, 描述, 封面URL等)
                update_task_status('DOWNLOADING', 10, '正在获取视频元数据...')
                result = subprocess.run(
                    [YTDLP_CMD, "--dump-json", "--cookies", "cookie.txt", link],
                    capture_output=True, text=True, check=True, encoding='utf-8'
                )
                video_data = json.loads(result.stdout)

                # 3. 下载封面图片
                update_task_status('DOWNLOADING', 20, '正在下载封面...')
                cover_url = video_data.get('thumbnail')
                cover_file_id = None
                if cover_url:
                    cover_file_id = str(uuid.uuid4())
                    file_ext = os.path.splitext(cover_url.split('?')[0])[-1] or '.jpg'  # 避免URL参数影响后缀判断
                    filename = f"{cover_file_id}{file_ext}"
                    filepath = os.path.join(app.config['UPLOAD_COVER'], secure_filename(filename))
                    response = requests.get(cover_url)
                    response.raise_for_status()
                    with open(filepath, 'wb') as f:
                        f.write(response.content)

                    # 将封面信息存入数据库
                    with sqlite3.connect(DB_NAME) as conn:
                        cursor = conn.cursor()
                        cursor.execute("INSERT INTO covers (id, original_filename, filepath) VALUES (?, ?, ?)",
                                       (cover_file_id, filename, filepath))
                        conn.commit()

                if is_download:
                    # 4. 下载视频文件
                    update_task_status('DOWNLOADING', 50, '正在下载视频...')
                    video_file_id = str(uuid.uuid4())
                    # 使用 yt-dlp 的输出模板确保文件名唯一且安全
                    video_filepath_template = os.path.join(app.config['UPLOAD_FOLDER'], f"{video_file_id}.%(ext)s")
                    cmd = [YTDLP_CMD, video_data.get('webpage_url'), "--remux-video", "mp4", "--cookies", "cookie.txt",
                           "-o", video_filepath_template]
                    result = subprocess.run(cmd, check=True, capture_output=True)

                    app.logger.info(result.stdout)
                    app.logger.info(result.stderr)

                    # 下载完成后，确定实际的文件路径（因为扩展名是动态的）
                    final_video_filename = f"{video_file_id}.mp4"
                    final_video_filepath = os.path.join(app.config['UPLOAD_FOLDER'], final_video_filename)
                    retry_count:int = 0
                    while not os.path.exists(final_video_filepath):
                        if retry_count < 10:# 等待20秒 如果还没有下载成功，就抛出异常
                            sleep(2)
                            retry_count += 1
                        else:
                            raise Exception(result.stderr)
                    # 将视频文件信息存入数据库
                    with sqlite3.connect(DB_NAME) as conn:
                        cursor = conn.cursor()
                        cursor.execute("INSERT INTO files (id, original_filename, filepath) VALUES (?, ?, ?)",
                                       (video_file_id, video_data.get('title', 'untitled'), final_video_filepath))
                        conn.commit()
                else:
                    video_file_id = None

                # 5. 将下载好的视频信息作为书签存入数据库
                update_task_status('DOWNLOADING', 90, '正在保存书签信息...')
                with sqlite3.connect(DB_NAME) as conn:
                    cursor = conn.cursor()
                    # 检查此链接是否已作为书签存在
                    existing_bookmark_id = check_duplicate_bookmark(video_data.get('webpage_url'))
                    if existing_bookmark_id:
                        bookmark_id = existing_bookmark_id
                    else:
                        bookmark_id = str(uuid.uuid4())
                        cursor.execute(
                            "INSERT INTO bookmarks (id, title, description, link, cover, file_id) VALUES (?, ?, ?, ?, ?, ?)",
                            (bookmark_id, video_data.get('title'), video_data.get('description'),
                             video_data.get('webpage_url'),
                             cover_file_id, video_file_id))

                    # 绑定书签到用户的指定文件夹
                    cursor.execute(
                        "INSERT OR IGNORE INTO user_folder_bookmarks (user_id, folder_id, bookmark_id) VALUES (?, ?, ?)",
                        (user_id, folder_id, bookmark_id))
                    conn.commit()

                # 6. 任务完成
                update_task_status('COMPLETED', 100, '任务成功完成', result=bookmark_id)

            except subprocess.CalledProcessError as e:
                error_message = f"命令执行失败: {e.stderr}"
                update_task_status('FAILED', 0, message=error_message)
                with sqlite3.connect(DB_NAME) as conn:
                    cursor = conn.cursor()
                    cursor.execute("DELETE FROM tasks WHERE id = ?",
                                   (task_id,))
                    conn.commit()
            except Exception as e:
                error_message = f"发生未知错误: {str(e)}"
                update_task_status('FAILED', 0, message=error_message)


# --------------------------------------------------------------------------- #
# Flask 路由 (API Endpoints)
# --------------------------------------------------------------------------- #

# --- 静态文件服务 ---
@app.route('/covers/<filename>')
def serve_covers(filename):
    """提供封面图片的访问路径。"""
    return send_from_directory(app.config['UPLOAD_COVER'], filename)


@app.route('/files/<filename>')
def serve_files(filename):
    """手动实现对文件的断点续传支持。"""
    file_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)

    # 检查文件是否存在
    if not os.path.exists(file_path):
        return "File not found", 404

    # 获取 Range 头
    range_header = request.headers.get('Range', None)

    # 如果没有 Range 头，则返回整个文件
    if not range_header:
        with open(file_path, 'rb') as f:
            return Response(f.read(), mimetype='application/octet-stream')

    # 解析 Range 头
    try:
        size = os.path.getsize(file_path)
        # 解析 "bytes=start-end"
        range_value = range_header.replace('bytes=', '')
        start, end = range_value.split('-')
        start = int(start) if start else 0
        end = int(end) if end else size - 1
    except ValueError:
        return 'Invalid Range', 400

    # 检查请求的范围是否有效
    if start >= size or end >= size or start > end:
        return '', 416  # 416 Range Not Satisfiable

    length = end - start + 1

    with open(file_path, 'rb') as f:
        f.seek(start)
        data = f.read(length)

    # 构建 206 响应
    resp = Response(data, mimetype='application/octet-stream', status=206)
    resp.headers.add('Content-Range', f'bytes {start}-{end}/{size}')
    resp.headers.add('Accept-Ranges', 'bytes')

    return resp


# --- 页面路由 ---
@app.route('/')
def home():
    """首页"""
    return render_template('complete.html')


@app.route('/index')
def complete():
    """备用首页"""
    return render_template('index.html')


@app.route('/css/complete.css')
def complete_css():
    """首页样式"""
    return render_template('css/complete.css')


@app.route('/js/complete.js')
def complete_js():
    """首页脚本"""
    return render_template('js/complete.js')


# --- 用户认证 API ---
@app.route('/register', methods=['POST'])
def register():
    """用户注册接口"""
    data = request.json
    email = data.get('email')
    password = data.get('password')
    if not email or not password:
        return jsonify({"success": False, "message": "邮箱和密码是必填项"}), 400

    try:
        user_id = str(uuid.uuid4())
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("INSERT INTO users (id, email, password) VALUES (?, ?, ?)", (user_id, email, password))
        conn.commit()
        return jsonify({"success": True, "user_id": user_id}), 201
    except sqlite3.IntegrityError:
        return jsonify({"success": False, "message": "该邮箱已被注册"}), 409
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/login', methods=['POST'])
def login():
    """用户登录接口"""
    data = request.json
    email = data.get('email')
    password = data.get('password')
    if not email or not password:
        return jsonify({"success": False, "message": "邮箱和密码是必填项"}), 400

    conn = get_db_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT id FROM users WHERE email = ? AND password = ?", (email, password))
    user = cursor.fetchone()
    if user:
        return jsonify({"success": True, "user_id": user['id']})
    else:
        return jsonify({"success": False, "message": "邮箱或密码错误"}), 401


# --- 数据获取 API (受保护) ---
@app.route('/get_main_folders', methods=['GET'])
@login_required
def get_main_folders_api(user_id):
    """获取所有主文件夹（顶级分类）。如果数据库为空，则自动创建默认分类。"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT id, name FROM main_folders")
        main_folders = [{"id": row['id'], "name": row['name']} for row in cursor.fetchall()]

        # 如果主文件夹为空，则初始化默认分类
        if not main_folders:
            main_types = TRANSLATIONS.get(language, TRANSLATIONS['en'])

            for name in main_types:
                main_folder_id = str(uuid.uuid4())
                cursor.execute("INSERT INTO main_folders (id, name) VALUES (?, ?)", (main_folder_id, name))
            conn.commit()
            # 重新查询
            cursor.execute("SELECT id, name FROM main_folders")
            main_folders = [{"id": row['id'], "name": row['name']} for row in cursor.fetchall()]

        return jsonify({"success": True, "folders": main_folders})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/get_sub_folders/<parent_id>', methods=['GET'])
@login_required
def get_sub_folders_api(user_id, parent_id):
    """获取指定父文件夹下的所有子文件夹。"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("""
            SELECT f.id, f.name FROM folders f
            JOIN user_folders uf ON f.id = uf.folder_id
            WHERE uf.user_id = ? AND f.parent_id = ?
        """, (user_id, parent_id))
        sub_folders = [{"id": row['id'], "name": row['name']} for row in cursor.fetchall()]
        return jsonify({"success": True, "folders": sub_folders})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/get_bookmarks/<folder_id>', methods=['GET'])
@login_required
def get_bookmarks_api(user_id, folder_id):
    """获取指定文件夹下的所有书签。"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # 联合查询书签、封面和文件表以获取完整信息
        cursor.execute("""
            SELECT b.id, b.title, b.description, b.link, c.filepath as cover_path, f.filepath as file_path
            FROM bookmarks b
            JOIN user_folder_bookmarks ufb ON b.id = ufb.bookmark_id
            LEFT JOIN covers c ON b.cover = c.id
            LEFT JOIN files f ON b.file_id = f.id
            WHERE ufb.user_id = ? AND ufb.folder_id = ?
        """, (user_id, folder_id))
        bookmarks = [{
            "id": row['id'],
            "title": row['title'],
            "description": row['description'],
            "link": row['link'],
            "cover": row['cover_path'],
            "filepath": row['file_path']
        } for row in cursor.fetchall()]
        return jsonify({"success": True, "bookmarks": bookmarks})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


# --- 数据创建/修改 API (受保护) ---

def _handle_upload(user_id, table_name, upload_dir):
    """
    处理文件上传的通用辅助函数，用于上传封面和普通文件。
    :param user_id: 当前操作的用户ID（用于鉴权）。
    :param table_name: 要插入数据的表名 ('covers' or 'files')。
    :param upload_dir: 文件要保存的目录。
    :return: Flask Response 对象。
    """
    if 'file' not in request.files:
        return jsonify({"success": False, "message": "请求中没有文件部分"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"success": False, "message": "没有选择文件"}), 400

    if file:
        original_filename = secure_filename(file.filename)
        file_id = str(uuid.uuid4())
        file_ext = os.path.splitext(original_filename)[1]
        new_filename = f"{file_id}{file_ext}"
        filepath = os.path.join(upload_dir, new_filename)
        file.save(filepath)

        try:
            conn = get_db_connection()
            cursor = conn.cursor()
            cursor.execute(
                f"INSERT INTO {table_name} (id, original_filename, filepath) VALUES (?, ?, ?)",
                (file_id, original_filename, filepath)
            )
            conn.commit()
            return jsonify({"success": True, "file_id": file_id, "file_path": filepath})
        except Exception as e:
            # 如果数据库写入失败，最好能删除已保存的文件以避免产生孤立文件
            if os.path.exists(filepath):
                os.remove(filepath)
            return jsonify({"success": False, "message": f"数据库写入失败: {str(e)}"}), 500
    return jsonify({"success": False, "message": "未知错误"}), 500


@app.route('/upload_file', methods=['POST'])
@login_required
def upload_file(user_id):
    """上传普通文件接口。"""
    return _handle_upload(user_id, 'files', app.config['UPLOAD_FOLDER'])


@app.route('/upload_cover', methods=['POST'])
@login_required
def upload_cover(user_id):
    """上传封面图片接口。"""
    return _handle_upload(user_id, 'covers', app.config['UPLOAD_COVER'])


@app.route('/create_folder', methods=['POST'])
@login_required
def create_folder(user_id):
    """创建子文件夹接口。"""
    data = request.json
    name = data.get('name')
    parent_id = data.get('parent_id')  # parent_id 是主文件夹的ID
    if not name or not parent_id:
        return jsonify({"success": False, "message": "文件夹名称和父级ID是必填项"}), 400

    try:
        folder_id = str(uuid.uuid4())
        conn = get_db_connection()
        cursor = conn.cursor()
        # 插入新文件夹
        cursor.execute("INSERT INTO folders (id, name, parent_id) VALUES (?, ?, ?)", (folder_id, name, parent_id))
        # 绑定文件夹到当前用户
        cursor.execute("INSERT OR IGNORE INTO user_folders (user_id, folder_id) VALUES (?, ?)", (user_id, folder_id))
        conn.commit()
        return jsonify({"success": True, "folder_id": folder_id}), 201
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/add_bookmark', methods=['POST'])
@login_required
def add_bookmark(user_id):
    """手动添加书签接口（不涉及自动下载）。"""
    data = request.json
    title = data.get('title')
    description = data.get('description')
    folder_id = data.get('folder_id')
    link = data.get('link')
    cover_id = data.get('cover')  # 应该是 cover_id
    file_id = data.get('file_id')

    if not all([title, description, folder_id, link, cover_id]):
        return jsonify({"success": False, "message": "标题、描述、文件夹ID、链接和封面ID是必填项"}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # 检查链接是否已存在，避免重复
        existing_bookmark_id = check_duplicate_bookmark(link)
        if existing_bookmark_id:
            bookmark_id = existing_bookmark_id
        else:
            bookmark_id = str(uuid.uuid4())
            cursor.execute(
                "INSERT INTO bookmarks (id, title, description, link, cover, file_id) VALUES (?, ?, ?, ?, ?, ?)",
                (bookmark_id, title, description, link, cover_id, file_id)
            )
        # 绑定书签到用户的文件夹
        cursor.execute(
            "INSERT OR IGNORE INTO user_folder_bookmarks (user_id, folder_id, bookmark_id) VALUES (?, ?, ?)",
            (user_id, folder_id, bookmark_id)
        )
        conn.commit()
        return jsonify({"success": True, "bookmark_id": bookmark_id}), 201
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


# --- 爬虫与后台任务 API (受保护) ---

def _create_and_start_download_task(link, user_id, folder_id, is_download):
    """
    创建下载任务记录并启动后台下载线程的辅助函数。
    :return: task_id 或 None (如果创建失败)
    """
    task_id = str(uuid.uuid4())
    try:
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO tasks (id, link, user_id, folder_id, status, progress, is_download) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (task_id, link, user_id, folder_id, 'PENDING', 0, is_download)
            )
            conn.commit()
        # 在新线程中运行下载任务
        threading.Thread(target=process_video_download, args=(task_id, link, folder_id, user_id, is_download)).start()
        return task_id
    except Exception:
        return None


@app.route('/craw_url', methods=['POST'])
@login_required
def craw_url(user_id):
    """
    提交单个视频URL进行下载。
    如果URL是YouTube播放列表，则自动提取所有视频并为每个视频创建下载任务。
    """
    data = request.json
    link = data.get('link')
    folder_id = data.get('folder_id')
    is_download: bool = data.get('is_download')
    if not link or not folder_id:
        return jsonify({"success": False, "message": "链接和文件夹ID是必填项"}), 400

    # 检查是否为YouTube播放列表
    result = subprocess.run(
        [YTDLP_CMD, "--flat-playlist", "--dump-single-json", "--cookies", "cookie.txt", link],
        capture_output=True, text=True, check=True, encoding='utf-8'
    )
    video_data = json.loads(result.stdout)

    is_playlist = video_data['_type'] == 'playlist'

    if not is_playlist:
        # 处理单个视频
        task_id = _create_and_start_download_task(link, user_id, folder_id, is_download)
        if task_id:
            return jsonify({"success": True, "message": "任务已成功提交", "task_id": task_id})
        else:
            return jsonify({"success": False, "message": "任务创建失败"}), 500
    else:
        # 处理播放列表
        try:
            # 使用 yt-dlp 获取播放列表中所有视频的URL
            result = subprocess.run(
                [YTDLP_CMD, "-J", "--flat-playlist", "--cookies", "cookie.txt", link],
                capture_output=True, text=True, check=True, encoding='utf-8'
            )
            playlist_data = json.loads(result.stdout)

            task_ids = []
            for entry in playlist_data.get('entries', []):
                video_url = entry.get('url')
                if video_url:
                    task_id = _create_and_start_download_task(video_url, user_id, folder_id, is_download)
                    if task_id:
                        task_ids.append(task_id)

            if not task_ids:
                return jsonify({"success": False, "message": "无法从播放列表提取视频或创建任务失败"}), 400

            return jsonify({"success": True, "message": f"{len(task_ids)}个任务已成功提交", "task_ids": task_ids})
        except subprocess.CalledProcessError as e:
            return jsonify({"success": False, "message": f"解析播放列表失败: {e.stderr}"}), 500
        except Exception as e:
            return jsonify({"success": False, "message": f"处理播放列表时出错: {str(e)}"}), 500


@app.route('/get_progress/<task_id>', methods=['GET'])
@login_required
def get_progress(user_id, task_id):
    """根据任务ID获取任务的当前进度和状态。"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT status, progress, message FROM tasks WHERE id = ? AND user_id = ?", (task_id, user_id))
        task = cursor.fetchone()
        if task:
            return jsonify({
                "success": True,
                "task_id": task_id,
                "status": task['status'],
                "progress": task['progress'],
                "message": task['message']
            })
        else:
            return jsonify({"success": False, "message": "未找到任务或您无权查看此任务"}), 404
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/recover_tasks', methods=['POST'])
@login_required
def recover_tasks(user_id):
    """
    (此接口仅用于查询) 获取指定文件夹下所有未完成的任务ID列表。
    真正的任务恢复逻辑在应用启动时自动执行。
    """
    data = request.json
    folder_id = data.get('folder_id')
    if not folder_id:
        return jsonify({"success": False, "message": "文件夹ID是必填项"}), 400

    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        # 查询所有不是 'COMPLETED' 状态的任务
        cursor.execute(
            "SELECT id FROM tasks WHERE user_id = ? AND folder_id = ? AND status != ?",
            (user_id, folder_id, 'COMPLETED')
        )
        tasks = cursor.fetchall()
        recovered_task_ids = [task['id'] for task in tasks]
        return jsonify({
            "success": True,
            "message": f"找到 {len(recovered_task_ids)} 个未完成的任务。",
            "task_ids": recovered_task_ids
        })
    except Exception as e:
        return jsonify({"success": False, "message": f"查询任务失败: {str(e)}"}), 500


# --------------------------------------------------------------------------- #
# 应用启动逻辑
# --------------------------------------------------------------------------- #
def recovery_all_unfinished_tasks():
    with app.app_context():
        """
        在应用启动时，查找所有未完成的任务（状态不是 COMPLETED）并重新启动它们。
        """
        app.logger.info("开始检查并恢复未完成的任务...")
        try:
            with sqlite3.connect(DB_NAME) as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT id, link, folder_id, user_id, is_download FROM tasks WHERE status != ?",
                               ('COMPLETED',))
                unfinished_tasks = cursor.fetchall()

                if not unfinished_tasks:
                    app.logger.info("没有需要恢复的任务。")
                    return

                app.logger.info(f"找到 {len(unfinished_tasks)} 个需要恢复的任务。")
                for task in unfinished_tasks:
                    task_id, link, folder_id, user_id, is_download = task
                    app.logger.info(f"重新提交任务: {task_id}")
                    # 将任务状态重置为 PENDING
                    cursor.execute("UPDATE tasks SET status = ? WHERE id = ?", ('PENDING', task_id))
                    conn.commit()
                    # 在新线程中重新启动下载
                    threading.Thread(target=process_video_download,
                                     args=(task_id, link, folder_id, user_id, is_download)).start()
        except Exception as e:
            app.logger.info(f"恢复任务时发生错误: {e}")


if __name__ == '__main__':
    # 1. 确保数据库和表已创建
    init_db()

    # 2. 在一个单独的后台线程中启动任务恢复程序，避免阻塞Web服务器启动
    recovery_thread = threading.Thread(target=recovery_all_unfinished_tasks)
    recovery_thread.daemon = True  # 设置为守护线程，主程序退出时它也会退出
    recovery_thread.start()
    # 3. 启动 Flask Web 服务器
    app.run(host='0.0.0.0', port=5000, debug=True)
