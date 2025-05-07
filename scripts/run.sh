#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if [ ! -d "$ROOT_DIR/out" ]; then
  "$ROOT_DIR/scripts/build.sh"
fi

java -cp "$ROOT_DIR/out" com.globallock.server.GlobalLockApplication "$@"

