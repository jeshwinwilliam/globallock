#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/out"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

SOURCE_FILES=("${(@f)$(find "$ROOT_DIR/src/main/java" -name "*.java" | sort)}")
TEST_FILES=("${(@f)$(find "$ROOT_DIR/src/test/java" -name "*.java" | sort)}")

javac -d "$OUT_DIR" "${SOURCE_FILES[@]}" "${TEST_FILES[@]}"

echo "Compiled classes into $OUT_DIR"

