@echo off
setlocal enabledelayedexpansion
title Knight Online v403 - J2ME MicroEmulator

cd /d "%~dp0"

set "JAR_TARGET=KnightOnline_v403.jar"
set "WIDTH=800"
set "HEIGHT=480"

:: Xu ly tham so dau vao
if not "%~1"=="" (
    if /i "%~x1"==".jar" (
        set "JAR_TARGET=%~1"
        if not "%~2"=="" if not "%~3"=="" (
            set "WIDTH=%~2"
            set "HEIGHT=%~3"
        )
    ) else (
        if not "%~2"=="" (
            set "WIDTH=%~1"
            set "HEIGHT=%~2"
        )
    )
)

if not exist "%JAR_TARGET%" (
    echo [LOI] Khong tim thay file %JAR_TARGET%
    pause
    exit /b 1
)

:: Tim Java Executable
set "JAVA_EXEC="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_EXEC=%JAVA_HOME%\bin\java.exe"
    )
)
if "%JAVA_EXEC%"=="" (
    where java >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        set "JAVA_EXEC=java"
    )
)

if "%JAVA_EXEC%"=="" (
    echo [LOI] Khong tim thay Java tren he thong!
    echo Vui long cai dat Java JRE hoac JDK 8 tro len, vi du OpenJDK Temurin 8 hoac 17.
    pause
    exit /b 1
)

:: Cau hinh tu dong .microemulator/config2.xml
set "CFG_DIR=%USERPROFILE%\.microemulator"
if not exist "%CFG_DIR%" mkdir "%CFG_DIR%"

set /a "WIN_W=%WIDTH%+16"
set /a "WIN_H=%HEIGHT%+58"

:: Chuan hoa duong dan file cho XML URL
set "FULL_JAR_PATH=%CD%\%JAR_TARGET%"
set "FULL_JAR_PATH=%FULL_JAR_PATH:\=/%"

(
echo ^<config^>
echo 	^<files^>
echo 		^<recent maxCapacity="10"^>
echo 			^<midlet^>
echo 				^<name^>KnightOnline Midlet Suite^</name^>
echo 				^<url^>file:///%FULL_JAR_PATH%^</url^>
echo 			^</midlet^>
echo 		^</recent^>
echo 	^</files^>
echo 	^<system-properties^>
echo 		^<system-property name="avetana.forceNativeLibrary" value="true"/^>
echo 	^</system-properties^>
echo 	^<devices^>
echo 		^<device default="true"^>
echo 			^<name^>Resizable device^</name^>
echo 			^<descriptor^>org/microemu/device/resizable/device.xml^</descriptor^>
echo 			^<rectangle^>
echo 				^<x^>0^</x^>
echo 				^<y^>0^</y^>
echo 				^<width^>%WIDTH%^</width^>
echo 				^<height^>%HEIGHT%^</height^>
echo 			^</rectangle^>
echo 		^</device^>
echo 	^</devices^>
echo 	^<windows^>
echo 		^<main onstart="true"^>
echo 			^<x^>100^</x^>
echo 			^<y^>100^</y^>
echo 			^<width^>%WIN_W%^</width^>
echo 			^<height^>%WIN_H%^</height^>
echo 		^</main^>
echo 	^</windows^>
echo ^</config^>
) > "%CFG_DIR%\config2.xml"

echo ==========================================================
echo   KHOI CHAY HIEP SI ONLINE (J2ME / MICROEMULATOR)
echo   Che do: Man hinh thuan tuy (Resizable - Khong vo DT)
echo   Kich thuoc: %WIDTH%x%HEIGHT% (Co the keo tha cua so)
echo   Target: %JAR_TARGET%
echo   Java:   %JAVA_EXEC%
echo ==========================================================

"%JAVA_EXEC%" -Dsun.awt.im.active=false -Dsun.java2d.uiScale=1.0 -Dawt.useSystemAAFontSettings=lcd -Dswing.aatext=true -jar microemulator-3.0.0.jar --resizableDevice %WIDTH% %HEIGHT% "%JAR_TARGET%"
if %ERRORLEVEL% neq 0 (
    echo [LOI] Game da dung voi ma loi %ERRORLEVEL%.
    pause
)
