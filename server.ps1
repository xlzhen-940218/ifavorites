# 强制 PowerShell 终端使用 UTF-8 编码，以避免中文乱码
chcp 65001

# 设置脚本在遇到错误时立即停止
$ErrorActionPreference = "Stop"

Write-Host "正在拉取最新代码..."
git pull

# 检查虚拟环境文件夹是否存在，如果不存在才创建
if (-not (Test-Path ".\venv")) {
    Write-Host "正在创建虚拟环境..."
    python -m venv venv
}

Write-Host "正在激活虚拟环境并安装依赖..."
# PowerShell 下激活虚拟环境的正确命令
.\venv\Scripts\Activate.ps1

# 确保 pip 和 setuptools 是最新的
python -m pip install --upgrade pip setuptools

# 安装所需的 Python 包
pip install flask requests selenium webdriver-manager

Write-Host "正在启动服务器..."
python server.py