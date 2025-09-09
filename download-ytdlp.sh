 #!/bin/bash

set -e  # 遇到错误退出

# 检查并安装 Python 和 Git
if ! command -v python3 &> /dev/null; then
    echo "Python3 未安装，正在安装..."
    if command -v apt &> /dev/null; then
        sudo apt update
        sudo apt install -y python3 python3-venv
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y python3
    else
        echo "不支持的包管理器，请手动安装 Python3"
        exit 1
    fi
fi

if ! command -v git &> /dev/null; then
    echo "Git 未安装，正在安装..."
    if command -v apt &> /dev/null; then
        sudo apt install -y git
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y git
    else
        echo "不支持的包管理器，请手动安装 Git"
        exit 1
    fi
fi

# 确保 python 命令指向 python3
if ! command -v python &> /dev/null; then
    if command -v python3 &> /dev/null; then
        ln -s $(which python3) /usr/local/bin/python
    else
        echo "无法创建 python 软链接"
        exit 1
    fi
fi

# 检查 yt-dlp 目录是否存在
if [ -d "yt-dlp" ]; then
    echo "yt-dlp 目录已存在，更新代码..."
    cd yt-dlp
    git fetch origin
    git reset --hard origin/master
else
    # 克隆仓库
    echo "克隆 yt-dlp 仓库..."
    git clone https://github.com/yt-dlp/yt-dlp.git
    cd yt-dlp
fi

# 创建虚拟环境并构建
# python -m venv venv
# source venv/bin/activate

python devscripts/install_deps.py --include pyinstaller
python devscripts/make_lazy_extractors.py
python -m bundle.pyinstaller

# 查找并重命名生成的可执行文件
echo "查找生成的可执行文件..."
if [ -f "dist/yt-dlp" ]; then
    # 如果已经是 yt-dlp，直接复制
    executable="dist/yt-dlp"
elif [ $(ls dist/ | grep -c '^yt-dlp_') -gt 0 ]; then
    # 查找 yt-dlp_ 开头的文件
    executable=$(ls dist/yt-dlp_* | head -n 1)
else
    # 查找任何可执行文件
    executable=$(find dist/ -type f -executable | head -n 1)
fi

if [ -z "$executable" ]; then
    echo "错误：找不到生成的可执行文件"
    exit 1
fi

echo "找到可执行文件: $executable"

# 复制文件到当前目录并重命名为 yt-dlp
cp "$executable" ../yt-dlp-exec
chmod +x ../yt-dlp-exec

echo "完成！yt-dlp 可执行文件已复制到当前目录"
