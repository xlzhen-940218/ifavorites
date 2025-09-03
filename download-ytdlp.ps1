# 检查并安装 Python 和 Git
if (-not (Test-Path "Env:\PYTHON_HOME")) {
    Write-Host "Python 未安装，正在安装..."
    # 移除有问题的 -h 参数，添加静默安装参数
    winget install --id Python.Python.3.12 --silent --accept-package-agreements --accept-source-agreements
    # 刷新环境变量
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Host "Git 未安装，正在安装..."
    winget install --id Git.Git --source winget -h
}

# 等待安装完成并刷新环境变量
Write-Host "刷新环境变量..."
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

# 检查 yt-dlp 目录是否存在
if (Test-Path "yt-dlp") {
    Write-Host "yt-dlp 目录已存在，更新代码..."
    cd yt-dlp
    git pull
} else {
    # 克隆仓库
    Write-Host "克隆 yt-dlp 仓库..."
    git clone https://github.com/yt-dlp/yt-dlp.git
    cd yt-dlp
}

# 创建虚拟环境并构建
python -m venv venv
.\venv\Scripts\activate.ps1

python devscripts/install_deps.py --include pyinstaller
python devscripts/make_lazy_extractors.py
python -m bundle.pyinstaller

# 查找并重命名生成的可执行文件
Write-Host "查找生成的可执行文件..."
$executable = Get-ChildItem -Path "dist" -Name "yt-dlp.exe" -File
if (-not $executable) {
    $executable = Get-ChildItem -Path "dist" -Name "*.exe" -File | Select-Object -First 1
}

if (-not $executable) {
    Write-Host "错误：找不到生成的可执行文件"
    exit 1
}

Write-Host "找到可执行文件: $executable"

# 复制文件到当前目录并保持为 yt-dlp.exe
Copy-Item "dist\$executable" "..\yt-dlp.exe"
Write-Host "完成！yt-dlp.exe 已复制到当前目录"