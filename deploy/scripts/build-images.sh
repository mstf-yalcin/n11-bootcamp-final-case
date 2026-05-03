#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/../../backend"

echo "==> Building all modules + Jib images..."
./mvnw -DskipTests package jib:dockerBuild

echo
echo "==> Done. Images:"
docker images "n11/*"
