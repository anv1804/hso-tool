#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo "   BIÊN DỊCH VÀ ĐÓNG GÓI LẠI (RE-PACK) KNIGHTONLINE JAR   "
echo "=========================================================="

mkdir -p bin

find src_java -name "*.java" > bin/sources.txt

if [ ! -s "bin/sources.txt" ]; then
    echo "[LỖI] Không tìm thấy file mã nguồn Java trong src_java!"
    exit 1
fi

COUNT=$(wc -l < bin/sources.txt)
echo "[1/2] Đang biên dịch $COUNT file mã nguồn Java bằng javac..."

javac -encoding UTF-8 -source 8 -target 8 -cp "microemulator-3.0.0.jar" -d "bin" @bin/sources.txt

echo "[2/2] Đang cập nhật classes vào KnightOnline_v403.jar..."
jar uf KnightOnline_v403.jar -C bin .

echo "=========================================================="
echo "[THÀNH CÔNG] Đã biên dịch và cập nhật KnightOnline_v403.jar!"
echo "Có thể chạy ngay bằng ./run_java.sh hoặc ./run_tool.sh."
echo "=========================================================="
