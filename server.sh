echo "fetch new code..."
git pull

echo "create virtual envs..."
# 创建虚拟环境并构建
python -m venv venv
source venv/bin/activate

echo "pip install flask requests"
pip install flask requests selenium webdriver-manager

echo "start server"
python server.py
