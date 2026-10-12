# Khởi chạy HSO Tool Studio trên Windows (PowerShell)
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "==========================================================" -ForegroundColor Yellow
Write-Host "    KHỞI CHẠY HSO TOOL STUDIO (GIAO DIỆN HIỆP SĨ ONLINE)  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Yellow

$PyCmd = $null
if (Get-Command python -ErrorAction SilentlyContinue) {
    $PyCmd = "python"
} elseif (Get-Command py -ErrorAction SilentlyContinue) {
    $PyCmd = "py"
}

if (-not $PyCmd) {
    Write-Host "[LỖI] Không tìm thấy Python trên hệ thống!" -ForegroundColor Red
    Write-Host "Vui lòng cài đặt Python (>= 3.8) từ https://www.python.org/" -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

# Kiểm tra thư viện Pillow
$checkPil = & $PyCmd -c "import PIL" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[THÔNG BÁO] Đang cài đặt thư viện Pillow..." -ForegroundColor Yellow
    & $PyCmd -m pip install -r requirements.txt
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[LỖI] Cài đặt dependencies thất bại!" -ForegroundColor Red
        Read-Host "Nhấn Enter để thoát..."
        exit 1
    }
}

Write-Host "Đang mở HSO Tool Studio..." -ForegroundColor Green
& $PyCmd hso_tool.py
