#!/bin/sh
set -eu
VERSION=8.9
DIST_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/custom-distributions/gradle-$VERSION"
if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  mkdir -p "$(dirname "$DIST_DIR")"
  ZIP="$(dirname "$DIST_DIR")/gradle-$VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then curl -fLsS "$URL" -o "$ZIP"; elif command -v wget >/dev/null 2>&1; then wget -q "$URL" -O "$ZIP"; else echo 'curl or wget required' >&2; exit 1; fi
  unzip -q -o "$ZIP" -d "$(dirname "$DIST_DIR")"
fi
exec "$DIST_DIR/bin/gradle" "$@"
