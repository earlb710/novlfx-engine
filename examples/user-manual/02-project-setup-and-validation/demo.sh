#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$script_dir"

while [[ "$repo_root" != "/" && ! -f "$repo_root/build.gradle" ]]; do
  repo_root="$(dirname "$repo_root")"
done

if [[ ! -f "$repo_root/build.gradle" ]]; then
  echo "Unable to locate novlfx-engine repository root from $script_dir" >&2
  exit 1
fi

cd "$repo_root"

./gradlew --no-daemon build
./gradlew --no-daemon runTestScreen
