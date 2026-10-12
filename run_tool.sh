#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================================="
echo "    KHỞI CHẠY HSO TOOL STUDIO (GIAO DIỆN HIỆP SĨ ONLINE)  "
echo "=========================================================="

exec python3 hso_tool.py
