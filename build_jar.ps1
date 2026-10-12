# Script biên dịch và đóng gói JAR trên Windows bằng PowerShell
$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "==========================================================" -ForegroundColor Yellow
Write-Host "    BIÊN DỊCH VÀ ĐÓNG GÓI LẠI (RE-PACK) KNIGHTONLINE JAR   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Yellow

$JavacExec = $null
$JarExec = $null

if ($env:JAVA_HOME) {
    $jh = $env:JAVA_HOME.TrimEnd('\', '/')
    if (Test-Path "$jh\bin\javac.exe") { $JavacExec = "$jh\bin\javac.exe" }
    if (Test-Path "$jh\bin\jar.exe") { $JarExec = "$jh\bin\jar.exe" }
}
if (-not $JavacExec -and (Get-Command javac -ErrorAction SilentlyContinue)) { $JavacExec = "javac" }
if (-not $JarExec -and (Get-Command jar -ErrorAction SilentlyContinue)) { $JarExec = "jar" }

if (-not $JavacExec) {
    Write-Host "[LỖI] Không tìm thấy javac! Vui lòng cài đặt JDK (>= 8)." -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

if (-not $JarExec) {
    Write-Host "[LỖI] Không tìm thấy jar command!" -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

$BinDir = Join-Path $ScriptDir "bin"
if (-not (Test-Path $BinDir)) {
    New-Item -ItemType Directory -Path $BinDir -Force | Out-Null
}

$Sources = Get-ChildItem -Path (Join-Path $ScriptDir "src_java") -Recurse -Filter *.java
if (-not $Sources -or $Sources.Count -eq 0) {
    Write-Host "[LỖI] Không tìm thấy file Java nào trong src_java!" -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

$SourcesFile = Join-Path $BinDir "sources.txt"
$sourceLines = $Sources | ForEach-Object { $_.FullName.Replace('\', '/') }
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllLines($SourcesFile, $sourceLines, $utf8NoBom)

Write-Host "[1/2] Đang biên dịch $($Sources.Count) file mã nguồn Java..." -ForegroundColor Yellow
$javacArgs = @("-encoding", "UTF-8", "-source", "8", "-target", "8", "-cp", "microemulator-3.0.0.jar", "-d", "bin", "@$SourcesFile")
& $JavacExec $javacArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "[LỖI] Biên dịch thất bại!" -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

Write-Host "[2/2] Đang cập nhật classes vào KnightOnline_v403.jar..." -ForegroundColor Yellow
& $JarExec uf "KnightOnline_v403.jar" -C "bin" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "[LỖI] Đóng gói JAR thất bại!" -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "[THÀNH CÔNG] Đã biên dịch và cập nhật KnightOnline_v403.jar!" -ForegroundColor Green
Write-Host "Khởi chạy ngay bằng run_java.bat hoặc run_tool.bat." -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Green
