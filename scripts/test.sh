#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

"$ROOT_DIR/scripts/build.sh"
java -ea -cp "$ROOT_DIR/out" com.globallock.GlobalLockTests

