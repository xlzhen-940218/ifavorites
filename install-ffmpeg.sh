#!/bin/bash

# Define the package name
PACKAGE_NAME="ffmpeg"

# Find the package manager
if command -v apt &> /dev/null; then
    PKG_MANAGER="apt"
elif command -v dnf &> /dev/null; then
    PKG_MANAGER="dnf"
elif command -v yum &> /dev/null; then
    PKG_MANAGER="yum"
else
    echo "无法找到支持的包管理器（apt, dnf, yum）。请手动安装 ffmpeg。"
    exit 1
fi

# Determine the installation command
INSTALL_CMD=""
if [ "$PKG_MANAGER" = "apt" ]; then
    INSTALL_CMD="$PKG_MANAGER update && $PKG_MANAGER install -y $PACKAGE_NAME"
elif [ "$PKG_MANAGER" = "dnf" ] || [ "$PKG_MANAGER" = "yum" ]; then
    INSTALL_CMD="$PKG_MANAGER install -y $PACKAGE_NAME"
fi

# Check if ffmpeg is already installed
if command -v ffmpeg &> /dev/null; then
    echo "FFmpeg 已经安装。"
else
    echo "正在使用 $PKG_MANAGER 安装 FFmpeg..."
    # Execute the installation command
    eval $INSTALL_CMD
    # Check if the installation was successful
    if [ $? -ne 0 ]; then
        echo "FFmpeg 安装失败。"
        exit 1
    fi
    echo "FFmpeg 安装成功。"
fi

# Find the location of the ffmpeg binary
FFMPEG_PATH=$(command -v ffmpeg)

if [ -n "$FFMPEG_PATH" ]; then
    # Get the directory of the current script
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

    echo "正在将 FFmpeg 可执行文件复制到当前目录..."
    cp "$FFMPEG_PATH" "$SCRIPT_DIR/ffmpeg"

    if [ $? -eq 0 ]; then
        echo "FFmpeg 已经成功复制到 $SCRIPT_DIR/ffmpeg。"
    else
        echo "无法复制 FFmpeg 可执行文件。"
        exit 1
    fi
else
    echo "未找到 ffmpeg 可执行文件。"
    exit 1
fi