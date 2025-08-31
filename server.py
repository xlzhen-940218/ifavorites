import os
import sqlite3
import uuid
from flask import Flask, request, jsonify, render_template, send_from_directory
from werkzeug.utils import secure_filename

app = Flask(__name__)
DB_NAME = 'bookmarks.db'
UPLOAD_FOLDER = 'files'
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

UPLOAD_COVER = 'covers'
app.config['UPLOAD_COVER'] = UPLOAD_COVER

# 創建文件上傳目錄
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

if not os.path.exists(UPLOAD_COVER):
    os.makedirs(UPLOAD_COVER)

def init_db():
    """
    初始化数据库和表
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
    # 检查文件是否存在以避免错误
    if not os.path.isfile(os.path.join(UPLOAD_COVER, filename)):
        return jsonify({"success": False, "message": "file not found!"}), 404

    try:
        return send_from_directory(UPLOAD_COVER, filename)
    except FileNotFoundError:
        # 再次捕获 FileNotFoundError，尽管上面的 os.path.isfile 已经检查过
        return jsonify({"success": False, "message": "file not found!"}), 404

@app.route('/files/<filename>')
def serve_files(filename):
    """
    根据文件名返回 covers 目录下的文件。
    """
    # 检查文件是否存在以避免错误
    if not os.path.isfile(os.path.join(UPLOAD_FOLDER, filename)):
        return jsonify({"success": False, "message": "file not found!"}), 404

    try:
        return send_from_directory(UPLOAD_FOLDER, filename)
    except FileNotFoundError:
        # 再次捕获 FileNotFoundError，尽管上面的 os.path.isfile 已经检查过
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
        # 保留原始文件擴展名
        file_ext = os.path.splitext(original_filename)[1]
        filename = f"{file_id}{file_ext}"
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], secure_filename(filename))
        file.save(filepath)

        # 儲存文件資訊到資料庫
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

# 函數8: 上傳文件 (真實邏輯)
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
        # 保留原始文件擴展名
        file_ext = os.path.splitext(original_filename)[1]
        filename = f"{file_id}{file_ext}"
        filepath = os.path.join(app.config['UPLOAD_COVER'], secure_filename(filename))
        file.save(filepath)

        # 儲存文件資訊到資料庫
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

            # 检查链接是否已存在
            existing_bookmark_id = check_duplicate_bookmark(link)
            if existing_bookmark_id:
                # 链接已存在，返回现有ID并绑定
                bookmark_id = existing_bookmark_id
            else:
                # 链接不存在，创建新的收藏
                bookmark_id = str(uuid.uuid4())
                cursor.execute("INSERT INTO bookmarks (id, title, description, link, cover, file_id) VALUES (?, ?, ?, ?, ?, ?)",
                               (bookmark_id, title, description, link, cover, file_id))

            # 函数7: 绑定用户、文件夹和收藏
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
            # 检查是否已存在
            cursor.execute("SELECT COUNT(*) FROM main_folders")
            if cursor.fetchone()[0] > 0:
                return jsonify({"success": True, "message": "主文件夹已存在，无需重复创建"})

            # 不存在则写入
            for name in main_types:
                main_folder_id = str(uuid.uuid4())
                cursor.execute("INSERT INTO main_folders (id, name) VALUES (?, ?)", (main_folder_id, name))
            conn.commit()
            return jsonify({"success": True, "message": "主文件夹创建成功"})
    except Exception as e:
        return jsonify({"success": False, "message": str(e)}), 500


@app.route('/')
def home():
    return render_template('index.html')

@app.route('/complete')
def complete():
    return render_template('complete.html')

if __name__ == '__main__':
    init_db()
    app.run(host='0.0.0.0',debug=True)
