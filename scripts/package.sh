#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

mvn -DskipTests package
rm -rf dist
mkdir -p dist

jpackage \
  --type app-image \
  --name cliflow \
  --input acli-app/target \
  --main-jar cliflow-sdk-0.1.0-SNAPSHOT-all.jar \
  --main-class io.github.redsun64.acli.app.Main \
  --dest dist

echo "Created self-contained application at: $ROOT_DIR/dist/cliflow"
