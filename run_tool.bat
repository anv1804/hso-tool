@echo off
setlocal enabledelayedexpansion
title HSO Tool Studio - Knight Online v403

cd /d "%~dp0"

echo ==========================================================
echo    KHOI CHAY HSO TOOL STUDIO (GIAO DIEN HIEP SI ONLINE)
echo ==========================================================

:: Kiem tra Python
set "PY_CMD="
where python >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "PY_CMD=python"
) else (
    where py >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        set "PY_CMD=py"
    )
)

if "%PY_CMD%"=="" (
    echo [LOI] Khong tim thay Python tren he thong!
    echo Vui long cai dat Python 3.8 tro len tu: https://www.python.org/
    echo Nho tich chon "Add Python to PATH" khi cai dat.
    pause
    exit /b 1
)

:: Kiem tra thu vien Pillow
%PY_CMD% -c "import PIL" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [THONG BAO] Dang cai dat thu vien phu thuoc Pillow...
    %PY_CMD% -m pip install -r requirements.txt
    if %ERRORLEVEL% neq 0 (
        echo [LOI] Khong the cai dat thu vien. Vui long chay: pip install -r requirements.txt
        pause
        exit /b 1
    )
)

:: Khoi chay tool
echo Dang mo HSO Tool Studio...
%PY_CMD% hso_tool.py
if %ERRORLEVEL% neq 0 (
    echo [LOI] Tool bi dung voi ma loi %ERRORLEVEL%.
    pause
)

exit /b 0
