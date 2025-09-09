echo "fetch new code..."
git pull

echo "create virtual envs..."
# 创建虚拟环境并构建
python -m venv venv
source venv/bin/activate

echo "pip install flask requests"
pip install flask requests

echo "check ffmpeg"
chmod +x ./install-ffmpeg.sh
./install-ffmpeg.sh

echo "check yt-dlp-exec"
chmod +x ./download-ytdlp.sh
./download-ytdlp.sh

echo "start server"
python server.py
