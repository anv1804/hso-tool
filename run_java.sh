#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Parse tham số:
# Cách 1: ./run_java.sh
# Cách 2: ./run_java.sh [file.jar]
# Cách 3: ./run_java.sh [file.jar] [width] [height]
# Cách 4: ./run_java.sh [width] [height]

JAR_TARGET="KnightOnline_v403.jar"
WIDTH=800
HEIGHT=480

if [[ "$1" =~ \.jar$ ]]; then
    JAR_TARGET="$1"
    if [ -n "$2" ] && [ -n "$3" ]; then
        WIDTH="$2"
        HEIGHT="$3"
    fi
elif [ -n "$1" ] && [ -n "$2" ]; then
    WIDTH="$1"
    HEIGHT="$2"
fi

if [ ! -f "$JAR_TARGET" ]; then
    echo "Lỗi: Không tìm thấy file $JAR_TARGET"
    exit 1
fi

# Đảm bảo cấu hình config2.xml của MicroEmulator luôn ưu tiên Resizable device (không vỏ điện thoại)
mkdir -p "$HOME/.microemulator"
if [ ! -f "$HOME/.microemulator/config2.xml" ] || ! grep -q 'descriptor>org/microemu/device/resizable/device.xml' "$HOME/.microemulator/config2.xml"; then
    cat << EOF > "$HOME/.microemulator/config2.xml"
<config>
	<files>
		<recent maxCapacity="10">
			<midlet>
				<name>KnightOnline Midlet Suite</name>
				<url>file://$SCRIPT_DIR/$JAR_TARGET</url>
			</midlet>
		</recent>
	</files>
	<system-properties>
		<system-property name="avetana.forceNativeLibrary" value="true"/>
	</system-properties>
	<devices>
		<device default="true">
			<name>Resizable device</name>
			<filename/>
			<descriptor>org/microemu/device/resizable/device.xml</descriptor>
			<rectangle>
				<x>0</x>
				<y>0</y>
				<width>$WIDTH</width>
				<height>$HEIGHT</height>
			</rectangle>
		</device>
	</devices>
	<windows>
		<main onstart="true">
			<x>100</x>
			<y>100</y>
			<width>$((WIDTH + 16))</width>
			<height>$((HEIGHT + 58))</height>
		</main>
	</windows>
</config>
EOF
fi

# Ưu tiên Java 8 để tương thích tối đa với MicroEmulator & MIDlet CLDC/MIDP
if [ -x "/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java" ]; then
    JAVA_EXEC="/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java"
else
    JAVA_EXEC="java"
fi

echo "=========================================================="
echo "  KHỞI CHẠY HIỆP SĨ ONLINE (J2ME / MICROEMULATOR)        "
echo "  Chế độ: Màn hình thuần túy (Resizable - Không vỏ ĐT)  "
echo "  Kích thước: ${WIDTH}x${HEIGHT} (Có thể kéo thả cửa sổ)       "
echo "  Target: $JAR_TARGET                                    "
echo "  Java:   $JAVA_EXEC                                     "
echo "=========================================================="

exec "$JAVA_EXEC" -jar microemulator-3.0.0.jar --resizableDevice "$WIDTH" "$HEIGHT" "$JAR_TARGET"
