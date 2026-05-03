#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# VM üzerinde çalışan deploy script'i. CI'dan SSH ile çağrılır.
#
# Beklenen environment:
#   IMAGE_REPO    europe-west1-docker.pkg.dev/PROJECT/n11
#   IMAGE_TAG     <git-sha>
#   DEPLOY_DIR    ~/n11 (compose dosyalarının ve .env'in olduğu dizin)
#
# Adımlar:
#   1) GAR'a auth (VM'in SA'sı GAR reader olmalı)
#   2) Yeni image'ları pull
#   3) Compose'u yeniden başlat (rolling, sadece değişen servisler)
# ----------------------------------------------------------------------------
set -euo pipefail

: "${IMAGE_REPO:?IMAGE_REPO is required}"
: "${IMAGE_TAG:?IMAGE_TAG is required}"
DEPLOY_DIR="${DEPLOY_DIR:-$HOME/n11}"

cd "$DEPLOY_DIR"

GAR_HOST="${IMAGE_REPO%%/*}"
echo "==> Configuring docker auth for $GAR_HOST"
gcloud auth configure-docker "$GAR_HOST" --quiet

echo "==> Pulling images (tag=$IMAGE_TAG)"
IMAGE_REPO="$IMAGE_REPO" IMAGE_TAG="$IMAGE_TAG" \
  docker compose -f docker-compose.yml -f docker-compose.gcp.yml pull

echo "==> Starting services"
IMAGE_REPO="$IMAGE_REPO" IMAGE_TAG="$IMAGE_TAG" \
  docker compose -f docker-compose.yml -f docker-compose.gcp.yml up -d --remove-orphans

echo "==> Pruning dangling images"
docker image prune -f

echo "==> Deploy done. Status:"
docker compose -f docker-compose.yml -f docker-compose.gcp.yml ps
