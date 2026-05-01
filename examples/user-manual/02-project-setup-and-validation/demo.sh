#!/usr/bin/env bash
set -euo pipefail

cd /path/to/novlfx-engine

./gradlew --no-daemon build
./gradlew --no-daemon runTestScreen
