@echo off
setlocal enabledelayedexpansion
title Build and Repack Knight Online v403 JAR

cd /d "%~dp0"

echo ==========================================================
echo    BIEN DICH VA DONG GOI LAI (RE-PACK) KNIGHTONLINE JAR   
echo ==========================================================

:: Tim javac va jar
set "JAVAC_EXEC="
set "JAR_EXEC="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_EXEC=%JAVA_HOME%\bin\javac.exe"
    if exist "%JAVA_HOME%\bin\jar.exe" set "JAR_EXEC=%JAVA_HOME%\bin\jar.exe"
)
if "%JAVAC_EXEC%"=="" (
    where javac >nul 2>&1
    if %ERRORLEVEL% equ 0 set "JAVAC_EXEC=javac"
)
if "%JAR_EXEC%"=="" (
    where jar >nul 2>&1
    if %ERRORLEVEL% equ 0 set "JAR_EXEC=jar"
)

if "%JAVAC_EXEC%"=="" (
    echo [LOI] Khong tim thay javac! Vui long cai dat JDK 8 tro len.
    pause
    exit /b 1
)

if "%JAR_EXEC%"=="" (
    echo [LOI] Khong tim thay jar command! Vui long cai dat JDK.
    pause
    exit /b 1
)

:: Tao thu muc bin neu chua co
if not exist "bin" mkdir "bin"

:: Tao danh sach sources.txt khong co BOM
powershell -NoProfile -Command "$utf8 = New-Object System.Text.UTF8Encoding($false); $src = Get-ChildItem -Path src_java -Recurse -Filter *.java; [System.IO.File]::WriteAllLines('bin/sources.txt', ($src | ForEach-Object { $_.FullName.Replace('\', '/') }), $utf8)"

if not exist "bin\sources.txt" (
    echo [LOI] Khong tim thay file ma nguon Java trong thu muc src_java!
    pause
    exit /b 1
)

echo [1/2] Dang bien dich ma nguon Java bang javac...
"%JAVAC_EXEC%" -encoding UTF-8 -source 8 -target 8 -cp "microemulator-3.0.0.jar" -d "bin" @bin\sources.txt
if %ERRORLEVEL% neq 0 (
    echo.
    echo [LOI] Bien dich that bai! Kiem tra cac loi tren.
    pause
    exit /b 1
)

echo [2/2] Dang cap nhat cac file .class vao KnightOnline_v403.jar...
"%JAR_EXEC%" uf KnightOnline_v403.jar -C bin .
if %ERRORLEVEL% neq 0 (
    echo.
    echo [LOI] Dong goi JAR that bai!
    pause
    exit /b 1
)

echo.
echo ==========================================================
echo [THANH CONG] Da bien dich va cap nhat KnightOnline_v403.jar!
echo Ban co the chay ngay bang run_java.bat hoac run_tool.bat.
echo ==========================================================
echo.
pause
