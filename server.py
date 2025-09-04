import asyncio
import json
import os
import sqlite3
import subprocess
import sys
import uuid
import threading
import time
from os import system

import requests
from flask import Flask, request, jsonify, render_template, send_from_directory
from werkzeug.utils import secure_filename, redirect

from get_youtube_list import get_video_urls

app = Flask(__name__)
DB_NAME = 'bookmarks.db'
UPLOAD_FOLDER = 'files'
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

UPLOAD_COVER = 'covers'
app.config['UPLOAD_COVER'] = UPLOAD_COVER

# 确定 yt-dlp 命令的路径
if sys.platform == "win32":
    ytdlp_cmd = "yt-dlp.exe"
else:
    ytdlp_cmd = "./yt-dlp-exec"

# 創建文件上傳目錄
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

if not os.path.exists(UPLOAD_COVER):
    os.makedirs(UPLOAD_COVER)

# 任务状态字典，用于存储任务进度（或者使用数据库）
task_status = {}


def init_db():
    """
    初始化数据库和表，新增tasks表
    """
    with sqlite3.connect(DB_NAME) as conn:
        cursor = conn.cursor()
        # 用户表 (user)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
        ''')
        # 文件表 (files) - 新增
        cursor.execute('''
                    CREATE TABLE IF NOT EXISTS files (
                        id TEXT PRIMARY KEY,
                        original_filename TEXT NOT NULL,
                        filepath TEXT NOT NULL
                    )
                ''')
        # 封面表 (covers) - 新增
        cursor.execute('''
                    CREATE TABLE IF NOT EXISTS covers (
                        id TEXT PRIMARY KEY,
                        original_filename TEXT NOT NULL,
                        filepath TEXT NOT NULL
                    )
                ''')
        # 文件夹表 (folder)，可以有父级ID
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS folders (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                parent_id TEXT,
                FOREIGN KEY (parent_id) REFERENCES folders(id)
            )
        ''')
        # 收藏表 (bookmark)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS bookmarks (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                description TEXT,
                link TEXT UNIQUE NOT NULL,
                cover TEXT,
                file_id TEXT
            )
        ''')
        # 主文件夹表 (main_folders)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS main_folders (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL UNIQUE
            )
        ''')
        # 用户-文件夹绑定表 (user_folder)
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS user_folders (
                user_id TEXT,
                folder_id TEXT,
                PRIMARY KEY (user_id, folder_id),
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (folder_id) REFERENCES folders(id)
            )
        ''')
        # 用户-文件夹-收藏绑定表 (user_folder_bookmark)
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
        # 任务表 (tasks) - 新增
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                link TEXT NOT NULL,
                user_id TEXT NOT NULL,
                folder_id TEXT NOT NULL,
                status TEXT NOT NULL,
                progress INTEGER,
                message TEXT,
                result TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        conn.commit()
        print("数据库初始化完成。")


def check_duplicate_bookmark(link):
    """
    检查收藏链接是否已存在
    """
    with sqlite3.connect(DB_NAME) as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM bookmarks WHERE link = ?", (link,))
        result = cursor.fetchone()
        return result[0] if result else None


def get_current_user_id():
    """
    從請求頭中獲取並驗證用戶ID
    """
    auth_header = request.headers.get('Authorization')
    if not auth_header or not auth_header.startswith('Bearer '):
        return None
    user_id = auth_header.split(' ')[1]

    with sqlite3.connect(DB_NAME) as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM users WHERE id = ?", (user_id,))
        if cursor.fetchone():
            return user_id
    return None


@app.route('/covers/<filename>')
def serve_covers(filename):
    """
    根据文件名返回 covers 目录下的文件。
    """
    if not os.path.isfile(os.path.join(UPLOAD_COVER, filename)):
        return jsonify({"success": False, "message": "file not found!"}), 404

    try:
        return send_from_directory(UPLOAD_COVER, filename)
    except FileNotFoundError:
        return jsonify({"success": False, "message": "file not found!"}), 404


@app.route('/files/<filename>')
def serve_files(filename):
    """
    根据文件名返回 files 目录下的文件。
    """
    if not os.path.isfile(os.path.join(UPLOAD_FOLDER, filename)):
        return jsonify({"success": False, "message": "file not found!"}), 404

    try:
        return send_from_directory(UPLOAD_FOLDER, filename)
    except FileNotFoundError:
        return jsonify({"success": False, "message": "file not found!"}), 404


# --- 新增的获取数据接口 ---
@app.route('/get_main_folders', methods=['GET'])
def get_main_folders_api():
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授权"}), 401

    try:
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT id, name FROM main_folders")
            main_folders = [{"id": row[0], "name": row[1]} for row in cursor.fetchall()]
            if len(main_folders) == 0:
                main_types = ["视频", "音频", "图片", "文档", "压缩文件", "其他"]
                cursor.execute("SELECT COUNT(*) FROM main_folders")
                if cursor.fetchone()[0] > 0:
                    return jsonify({"success": True, "message": "主文件夹已存在，无需重复创建"})
                for name in main_types:
                    main_folder_id = str(uuid.uuid4())
                    cursor.execute("INSERT INTO main_folders (id, name) VALUES (?, ?)", (main_folder_id, name))
                conn.commit()
                cursor.execute("SELECT id, name FROM main_folders")
                main_folders = [{"id": row[0], "name": row[1]} for row in cursor.fetchall()]
            return jsonify({"success": True, "folders": main_folders})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/get_sub_folders/<parent_id>', methods=['GET'])
def get_sub_folders_api(parent_id):
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授权"}), 401

    try:
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT f.id, f.name
                FROM folders f
                JOIN user_folders uf ON f.id = uf.folder_id
                WHERE uf.user_id = ? AND f.parent_id = ?
            """, (user_id, parent_id))
            sub_folders = [{"id": row[0], "name": row[1]} for row in cursor.fetchall()]
            return jsonify({"success": True, "folders": sub_folders})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/get_bookmarks/<folder_id>', methods=['GET'])
def get_bookmarks_api(folder_id):
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授权"}), 401

    try:
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute("""
                SELECT b.title, b.description, b.link, c.filepath, f.filepath, b.id
                FROM bookmarks b
                JOIN user_folder_bookmarks ufb ON b.id = ufb.bookmark_id
                LEFT JOIN files f ON b.file_id = f.id
                LEFT JOIN covers c ON b.cover = c.id
                WHERE ufb.user_id = ? AND ufb.folder_id = ?
            """, (user_id, folder_id))
            bookmarks = [{
                "title": row[0],
                "description": row[1],
                "link": row[2],
                "cover": row[3],
                "filepath": row[4],
                "id": row[5]
            } for row in cursor.fetchall()]
            return jsonify({"success": True, "bookmarks": bookmarks})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


# 函數8: 上傳文件 (真實邏輯)
@app.route('/upload_file', methods=['POST'])
def upload_file():
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授權"}), 401

    if 'file' not in request.files:
        return jsonify({"success": False, "message": "沒有文件部分"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"success": False, "message": "沒有選擇文件"}), 400
    if file:
        original_filename = file.filename
        file_id = str(uuid.uuid4())
        file_ext = os.path.splitext(original_filename)[1]
        filename = f"{file_id}{file_ext}"
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(filename))
        file.save(filepath)

        try:
            with sqlite3.connect(DB_NAME) as conn:
                cursor = conn.cursor()
                cursor.execute(
                    "INSERT INTO files (id, original_filename, filepath) VALUES (?, ?, ?)",
                    (file_id, original_filename, filepath)
                )
                conn.commit()
                return jsonify({"success": True, "file_id": file_id, "file_path": filepath})
        except Exception as e:
            return jsonify({"success": False, "message": f"資料庫寫入失敗: {str(e)}"}), 500
    return None


@app.route('/upload_cover', methods=['POST'])
def upload_cover():
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授權"}), 401

    if 'file' not in request.files:
        return jsonify({"success": False, "message": "沒有文件部分"}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({"success": False, "message": "沒有選擇文件"}), 400
    if file:
        original_filename = file.filename
        file_id = str(uuid.uuid4())
        file_ext = os.path.splitext(original_filename)[1]
        filename = f"{file_id}{file_ext}"
        filepath = os.path.join(app.config['UPLOAD_COVER'], secure_filename(filename))
        file.save(filepath)

        try:
            with sqlite3.connect(DB_NAME) as conn:
                cursor = conn.cursor()
                cursor.execute(
                    "INSERT INTO covers (id, original_filename, filepath) VALUES (?, ?, ?)",
                    (file_id, original_filename, filepath)
                )
                conn.commit()
                return jsonify({"success": True, "file_id": file_id, "file_path": filepath})
        except Exception as e:
            return jsonify({"success": False, "message": f"資料庫寫入失敗: {str(e)}"}), 500
    return None


# 函数4: 用户注册
@app.route('/register', methods=['POST'])
def register():
    data = request.json
    email = data.get('email')
    password = data.get('password')
    if not email or not password:
        return jsonify({"success": False, "message": "邮箱和密码是必填项"}), 400

    try:
        user_id = str(uuid.uuid4())
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO users (id, email, password) VALUES (?, ?, ?)", (user_id, email, password))
            conn.commit()
            return jsonify({"success": True, "user_id": user_id})
    except sqlite3.IntegrityError:
        return jsonify({"success": False, "message": "该邮箱已被注册"}), 409
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


# 函数5: 用户登录
@app.route('/login', methods=['POST'])
def login():
    data = request.json
    email = data.get('email')
    password = data.get('password')
    if not email or not password:
        return jsonify({"success": False, "message": "邮箱和密码是必填项"}), 400

    with sqlite3.connect(DB_NAME) as conn:
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM users WHERE email = ? AND password = ?", (email, password))
        result = cursor.fetchone()
        if result:
            user_id = result[0]
            return jsonify({"success": True, "user_id": user_id})
        else:
            return jsonify({"success": False, "message": "邮箱或密码错误"}), 401


# 函数2: 创建二级菜单（文件夹）
@app.route('/create_folder', methods=['POST'])
def create_folder():
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授權"}), 401

    data = request.json
    name = data.get('name')
    parent_id = data.get('parent_id')
    user_id = data.get('user_id')
    if not name or not user_id or not parent_id:
        return jsonify({"success": False, "message": "文件夹名称和用户ID,父级ID是必填项"}), 400

    try:
        folder_id = str(uuid.uuid4())
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO folders (id, name, parent_id) VALUES (?, ?, ?)", (folder_id, name, parent_id))
            # 函数6: 绑定用户和文件夹
            cursor.execute("INSERT OR IGNORE INTO user_folders (user_id, folder_id) VALUES (?, ?)",
                           (user_id, folder_id))
            conn.commit()
            return jsonify({"success": True, "folder_id": folder_id})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


# 函数1: 写入收藏
@app.route('/add_bookmark', methods=['POST'])
def add_bookmark():
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授權"}), 401

    data = request.json
    title = data.get('title')
    description = data.get('description')
    folder_id = data.get('folder_id')
    link = data.get('link')
    cover = data.get('cover')
    user_id = data.get('user_id')
    file_id = data.get('file_id')  # 可选

    if not title or not folder_id or not link or not user_id or not description or not cover:
        return jsonify({"success": False, "message": "标题、描述、文件夹ID、链接和用户ID是必填项"}), 400

    try:
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            existing_bookmark_id = check_duplicate_bookmark(link)
            if existing_bookmark_id:
                bookmark_id = existing_bookmark_id
            else:
                bookmark_id = str(uuid.uuid4())
                cursor.execute(
                    "INSERT INTO bookmarks (id, title, description, link, cover, file_id) VALUES (?, ?, ?, ?, ?, ?)",
                    (bookmark_id, title, description, link, cover, file_id))

            cursor.execute(
                "INSERT OR IGNORE INTO user_folder_bookmarks (user_id, folder_id, bookmark_id) VALUES (?, ?, ?)",
                (user_id, folder_id, bookmark_id))
            conn.commit()
            return jsonify({"success": True, "bookmark_id": bookmark_id})

    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


# 函数3: 写入主文件夹
@app.route('/create_main_folders', methods=['POST'])
def create_main_folders():
    main_types = ["视频", "音频", "图片", "文档", "压缩文件", "其他"]
    try:
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM main_folders")
            if cursor.fetchone()[0] > 0:
                return jsonify({"success": True, "message": "主文件夹已存在，无需重复创建"})
            for name in main_types:
                main_folder_id = str(uuid.uuid4())
                cursor.execute("INSERT INTO main_folders (id, name) VALUES (?, ?)", (main_folder_id, name))
            conn.commit()
            return jsonify({"success": True, "message": "主文件夹创建成功"})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/')
def home():
    return render_template('complete.html')


@app.route('/index')
def complete():
    return render_template('index.html')


def process_video_download(task_id, link, folder_id, user_id):
    """
    后台线程处理视频下载任务
    """

    def update_task_status(status, progress=None, message=None, result=None):
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            update_query = "UPDATE tasks SET status = ?, message = ?, result = ? WHERE id = ?"
            params = [status, message, result, task_id]
            if progress is not None:
                update_query = "UPDATE tasks SET status = ?, progress = ?, message = ?, result = ? WHERE id = ?"
                params = [status, progress, message, result, task_id]
            cursor.execute(update_query, tuple(params))
            conn.commit()

    try:
        # 1. 更新任务状态为处理中
        update_task_status('DOWNLOADING', 0)

        # 2. 获取视频元数据
        update_task_status('DOWNLOADING', 10, message='获取视频元数据...')
        result = subprocess.run([ytdlp_cmd, "--dump-json", link], capture_output=True, text=True, check=True)
        video_data = json.loads(result.stdout)

        # 3. 下载封面
        update_task_status('DOWNLOADING', 20, message='下载封面...')
        cover_file_id = str(uuid.uuid4())
        file_ext = os.path.splitext(video_data['thumbnail'])[1]
        filename = f"{cover_file_id}{file_ext}"
        filepath = os.path.join(app.config['UPLOAD_COVER'], secure_filename(filename))
        response = requests.get(video_data['thumbnail'])
        with open(filepath, 'wb') as f:
            f.write(response.content)

        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO covers (id, original_filename, filepath) VALUES (?, ?, ?)",
                           (cover_file_id, filename, filepath))
            conn.commit()

        # 4. 下载视频
        update_task_status('DOWNLOADING', 50, message='下载视频...')
        video_file_id = str(uuid.uuid4())
        video_filename = f"{video_file_id}.mp4"
        video_filepath = os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(video_filename))
        cmd = [ytdlp_cmd, link, "--remux-video", "mp4", "-o", video_filepath]
        subprocess.run(cmd, check=True, capture_output=True, text=True)

        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute("INSERT INTO files (id, original_filename, filepath) VALUES (?, ?, ?)",
                           (video_file_id, video_filename, video_filepath))
            conn.commit()

        # 5. 添加书签到数据库
        update_task_status('DOWNLOADING', 90, message='保存书签...')
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            existing_bookmark_id = check_duplicate_bookmark(link)
            if existing_bookmark_id:
                bookmark_id = existing_bookmark_id
            else:
                bookmark_id = str(uuid.uuid4())
                cursor.execute(
                    "INSERT INTO bookmarks (id, title, description, link, cover, file_id) VALUES (?, ?, ?, ?, ?, ?)",
                    (bookmark_id, video_data['title'], video_data['description'], video_data['webpage_url'],
                     cover_file_id, video_file_id))

            cursor.execute(
                "INSERT OR IGNORE INTO user_folder_bookmarks (user_id, folder_id, bookmark_id) VALUES (?, ?, ?)",
                (user_id, folder_id, bookmark_id))
            conn.commit()

        # 6. 任务完成
        update_task_status('COMPLETED', 100, message='下载完成', result=bookmark_id)
    except subprocess.CalledProcessError as e:
        update_task_status('FAILED', 0, message=f"命令执行失败: {e.stderr}", result=None)
    except Exception as e:
        update_task_status('FAILED', 0, message=f"任务失败: {str(e)}", result=None)


@app.route('/craw_url', methods=['POST'])
def craw_url():
    """
    提交下载任务到后台
    """
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授权"}), 401

    data = request.json
    link = data.get('link')
    folder_id = data.get('folder_id')

    if not link or not folder_id:
        return jsonify({"success": False, "message": "链接和文件夹ID是必填项"}), 400

    if '&list' in link and 'www.youtube.com' in link:
        return redirect('craw_list', code=307)

    task_id = str(uuid.uuid4())
    try:
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO tasks (id, link, user_id, folder_id, status, progress) VALUES (?, ?, ?, ?, ?, ?)",
                (task_id, link, user_id, folder_id, 'PENDING', 0)
            )
            conn.commit()
    except Exception as e:
        return jsonify({"success": False, "message": f"任务创建失败: {str(e)}"}), 500

    # 在新线程中运行下载任务
    threading.Thread(target=process_video_download, args=(task_id, link, folder_id, user_id)).start()

    return jsonify({"success": True, "message": "任务已成功提交", "task_id": task_id})


@app.route('/get_progress/<task_id>', methods=['GET'])
def get_progress(task_id):
    """
    根据任务ID获取任务进度
    """
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授权"}), 401

    try:
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT status, progress, message FROM tasks WHERE id = ? AND user_id = ?",
                           (task_id, user_id))
            result = cursor.fetchone()
            if result:
                status, progress, message = result
                return jsonify({
                    "success": True,
                    "task_id": task_id,
                    "status": status,
                    "progress": progress,
                    "message": message
                })
            else:
                return jsonify({"success": False, "message": "未找到任务或您无权查看此任务"}), 404
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500

@app.route('/craw_list', methods=['POST'])
def craw_list():
    """
        提交下载任务到后台
        """
    user_id = get_current_user_id()
    if not user_id:
        return jsonify({"success": False, "message": "未授权"}), 401

    data = request.json
    link = data.get('link')
    folder_id = data.get('folder_id')

    if not link or not folder_id:
        return jsonify({"success": False, "message": "链接和文件夹ID是必填项"}), 400

    if not '&list' in link or not 'www.youtube.com' in link:
        return jsonify({"success": False, "message": "url is not youtube play list!"}), 404

    links = get_video_urls(link)
    task_ids = []
    for link in links:
        task_id = str(uuid.uuid4())
        try:
            with sqlite3.connect(DB_NAME) as conn:
                cursor = conn.cursor()
                cursor.execute(
                    "INSERT INTO tasks (id, link, user_id, folder_id, status, progress) VALUES (?, ?, ?, ?, ?, ?)",
                    (task_id, link, user_id, folder_id, 'PENDING', 0)
                )
                conn.commit()
        except Exception as e:
            return jsonify({"success": False, "message": f"任务创建失败: {str(e)}"}), 500

        # 在新线程中运行下载任务
        threading.Thread(target=process_video_download, args=(task_id, link, folder_id, user_id)).start()
        task_ids.append(task_id)

    return jsonify({"success": True, "message": "任务已成功提交", "task_ids": task_ids})



if __name__ == '__main__':
    init_db()
    app.run(host='0.0.0.0', debug=True)
