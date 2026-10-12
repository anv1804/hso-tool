#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAR_TARGET="${1:-KnightOnline_v403.jar}"

if [ ! -f "$JAR_TARGET" ]; then
    echo "Lỗi: Không tìm thấy file $JAR_TARGET"
    exit 1
fi

# Ưu tiên Java 8 để tương thích tối đa với MicroEmulator & MIDlet CLDC/MIDP
if [ -x "/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java" ]; then
    JAVA_EXEC="/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java"
else
    JAVA_EXEC="java"
fi

echo "=========================================================="
echo "  KHỞI CHẠY HIỆP SĨ ONLINE (J2ME / MICROEMULATOR)        "
echo "  Target: $JAR_TARGET                                    "
echo "  Java:   $JAVA_EXEC                                     "
echo "=========================================================="

exec "$JAVA_EXEC" -jar microemulator-3.0.0.jar "$JAR_TARGET"
