# Khởi chạy Hiệp Sĩ Online (J2ME / MicroEmulator) trên Windows qua PowerShell
param(
    [string]$Arg1 = "",
    [string]$Arg2 = "",
    [string]$Arg3 = ""
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$JarTarget = "KnightOnline_v403.jar"
$Width = 800
$Height = 480

if ($Arg1 -match '\.jar$') {
    $JarTarget = $Arg1
    if ($Arg2 -and $Arg3) {
        $Width = [int]$Arg2
        $Height = [int]$Arg3
    }
} elseif ($Arg1 -and $Arg2) {
    $Width = [int]$Arg1
    $Height = [int]$Arg2
}

if (-not (Test-Path $JarTarget)) {
    Write-Host "[LỖI] Không tìm thấy file $JarTarget" -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

$JavaExec = $null
if ($env:JAVA_HOME) {
    $jh = $env:JAVA_HOME.TrimEnd('\', '/')
    if (Test-Path "$jh\bin\java.exe") {
        $JavaExec = "$jh\bin\java.exe"
    }
} elseif (Get-Command java -ErrorAction SilentlyContinue) {
    $JavaExec = "java"
}

if (-not $JavaExec) {
    Write-Host "[LỖI] Không tìm thấy Java trên hệ thống!" -ForegroundColor Red
    Read-Host "Nhấn Enter để thoát..."
    exit 1
}

# Cập nhật config2.xml của MicroEmulator
$CfgDir = Join-Path $HOME ".microemulator"
if (-not (Test-Path $CfgDir)) {
    New-Item -ItemType Directory -Path $CfgDir -Force | Out-Null
}

$FullJarPath = (Resolve-Path $JarTarget).Path.Replace('\', '/')
$WinW = $Width + 16
$WinH = $Height + 58

$xml = @"
<config>
	<files>
		<recent maxCapacity="10">
			<midlet>
				<name>KnightOnline Midlet Suite</name>
				<url>file:///$FullJarPath</url>
			</midlet>
		</recent>
	</files>
	<system-properties>
		<system-property name="avetana.forceNativeLibrary" value="true"/>
	</system-properties>
	<devices>
		<device default="true">
			<name>Resizable device</name>
			<descriptor>org/microemu/device/resizable/device.xml</descriptor>
			<rectangle>
				<x>0</x>
				<y>0</y>
				<width>$Width</width>
				<height>$Height</height>
			</rectangle>
		</device>
	</devices>
	<windows>
		<main onstart="true">
			<x>100</x>
			<y>100</y>
			<width>$WinW</width>
			<height>$WinH</height>
		</main>
	</windows>
</config>
"@

[System.IO.File]::WriteAllText((Join-Path $CfgDir "config2.xml"), $xml, [System.Text.Encoding]::UTF8)

Write-Host "==========================================================" -ForegroundColor Yellow
Write-Host "  KHỞI CHẠY HIỆP SĨ ONLINE (J2ME / MICROEMULATOR)        " -ForegroundColor Cyan
Write-Host "  Chế độ: Màn hình thuần túy (Resizable - Không vỏ ĐT)  " -ForegroundColor Green
Write-Host "  Kích thước: ${Width}x${Height} (Có thể kéo thả cửa sổ)       " -ForegroundColor Green
Write-Host "  Target: $JarTarget                                    " -ForegroundColor White
Write-Host "  Java:   $JavaExec                                     " -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Yellow

& $JavaExec "-Dsun.awt.im.active=false" "-Dsun.java2d.uiScale=1.0" "-Dawt.useSystemAAFontSettings=lcd" "-Dswing.aatext=true" -jar microemulator-3.0.0.jar --resizableDevice $Width $Height $JarTarget
