#!/usr/bin/env bash
# ---------------------------------------------------------------------------
#   1) Gerekli API'leri etkinleştirir (artifactregistry, compute, iamcredentials)
#   2) Artifact Registry repo'su oluşturur
#   3) CI service account'u oluşturur (push + deploy yetkili)
#   4) Workload Identity Federation pool + provider kurar (GitHub OIDC için)
#   5) GitHub repo'suna SA impersonation izni verir
#   6) GCE VM oluşturur (VM'in kendi SA'sı GAR reader)
#   7) VM'e docker + compose plugin yükler ve compose dosyalarını kopyalar
#
# Çalıştırma:
#   GCP_PROJECT_ID=my-project \
#   GCP_REGION=europe-west1 \
#   GCP_ZONE=europe-west1-b \
#   GITHUB_REPO=mstf-yalcin/n11bootcamp-final \
#   bash deploy/scripts/gcp-bootstrap.sh
#
# ----------------------------------------------------------------------------
set -euo pipefail

: "${GCP_PROJECT_ID:?GCP_PROJECT_ID is required}"
: "${GCP_REGION:=europe-west1}"
: "${GCP_ZONE:=${GCP_REGION}-b}"
: "${GITHUB_REPO:?GITHUB_REPO is required)}"
: "${AR_REPO:=n11}"
: "${VM_NAME:=n11-vm}"
: "${VM_MACHINE:=e2-standard-4}"
: "${WIF_POOL:=github-pool}"
: "${WIF_PROVIDER:=github-provider}"
: "${CI_SA_NAME:=n11-ci}"
: "${VM_SA_NAME:=n11-vm}"

PROJECT_NUMBER="$(gcloud projects describe "$GCP_PROJECT_ID" --format='value(projectNumber)')"
CI_SA_EMAIL="${CI_SA_NAME}@${GCP_PROJECT_ID}.iam.gserviceaccount.com"
VM_SA_EMAIL="${VM_SA_NAME}@${GCP_PROJECT_ID}.iam.gserviceaccount.com"

gcloud config set project "$GCP_PROJECT_ID" >/dev/null

echo "==> Enabling APIs"
gcloud services enable \
  artifactregistry.googleapis.com \
  compute.googleapis.com \
  iamcredentials.googleapis.com \
  iam.googleapis.com

echo "==> Creating Artifact Registry repo: $AR_REPO ($GCP_REGION)"
gcloud artifacts repositories describe "$AR_REPO" --location="$GCP_REGION" >/dev/null 2>&1 || \
  gcloud artifacts repositories create "$AR_REPO" \
    --repository-format=docker \
    --location="$GCP_REGION" \
    --description="n11 bootcamp images"

echo "==> Creating CI service account: $CI_SA_EMAIL"
gcloud iam service-accounts describe "$CI_SA_EMAIL" >/dev/null 2>&1 || \
  gcloud iam service-accounts create "$CI_SA_NAME" --display-name="n11 CI"

echo "==> Granting CI SA: GAR writer + GCE OS Login + IAP tunnel"
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member="serviceAccount:${CI_SA_EMAIL}" \
  --role="roles/artifactregistry.writer" --condition=None --quiet >/dev/null
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member="serviceAccount:${CI_SA_EMAIL}" \
  --role="roles/compute.osLogin" --condition=None --quiet >/dev/null
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member="serviceAccount:${CI_SA_EMAIL}" \
  --role="roles/iap.tunnelResourceAccessor" --condition=None --quiet >/dev/null
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member="serviceAccount:${CI_SA_EMAIL}" \
  --role="roles/compute.instanceAdmin.v1" --condition=None --quiet >/dev/null

echo "==> Creating VM service account: $VM_SA_EMAIL"
gcloud iam service-accounts describe "$VM_SA_EMAIL" >/dev/null 2>&1 || \
  gcloud iam service-accounts create "$VM_SA_NAME" --display-name="n11 VM"

echo "==> Granting VM SA: GAR reader"
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
  --member="serviceAccount:${VM_SA_EMAIL}" \
  --role="roles/artifactregistry.reader" --condition=None --quiet >/dev/null

echo "==> CI SA can use VM SA (for OS Login SSH)"
gcloud iam service-accounts add-iam-policy-binding "$VM_SA_EMAIL" \
  --member="serviceAccount:${CI_SA_EMAIL}" \
  --role="roles/iam.serviceAccountUser" --quiet >/dev/null

echo "==> Setting up Workload Identity Federation"
gcloud iam workload-identity-pools describe "$WIF_POOL" --location=global >/dev/null 2>&1 || \
  gcloud iam workload-identity-pools create "$WIF_POOL" \
    --location=global --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers describe "$WIF_PROVIDER" \
  --workload-identity-pool="$WIF_POOL" --location=global >/dev/null 2>&1 || \
  gcloud iam workload-identity-pools providers create-oidc "$WIF_PROVIDER" \
    --workload-identity-pool="$WIF_POOL" \
    --location=global \
    --display-name="GitHub OIDC" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
    --attribute-condition="assertion.repository_owner == '${GITHUB_REPO%%/*}'"

WIF_PROVIDER_ID="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${WIF_POOL}/providers/${WIF_PROVIDER}"

echo "==> Binding GitHub repo ($GITHUB_REPO) to CI SA"
gcloud iam service-accounts add-iam-policy-binding "$CI_SA_EMAIL" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${WIF_POOL}/attribute.repository/${GITHUB_REPO}" \
  --quiet >/dev/null

echo "==> Creating VM: $VM_NAME ($VM_MACHINE in $GCP_ZONE)"
if ! gcloud compute instances describe "$VM_NAME" --zone="$GCP_ZONE" >/dev/null 2>&1; then
  gcloud compute instances create "$VM_NAME" \
    --zone="$GCP_ZONE" \
    --machine-type="$VM_MACHINE" \
    --image-family=ubuntu-2404-lts-amd64 \
    --image-project=ubuntu-os-cloud \
    --boot-disk-size=50GB \
    --boot-disk-type=pd-balanced \
    --service-account="$VM_SA_EMAIL" \
    --scopes=cloud-platform \
    --metadata=enable-oslogin=TRUE \
    --tags=n11-vm
fi

echo "==> Opening firewall (frontend 80, gateway 8080)"
gcloud compute firewall-rules describe n11-allow-http >/dev/null 2>&1 || \
  gcloud compute firewall-rules create n11-allow-http \
    --direction=INGRESS --action=ALLOW \
    --rules=tcp:80,tcp:8080 \
    --target-tags=n11-vm \
    --source-ranges=0.0.0.0/0

echo "==> Installing docker on VM (idempotent)"
gcloud compute ssh "$VM_NAME" --zone="$GCP_ZONE" --tunnel-through-iap --command='
  set -e
  if ! command -v docker >/dev/null; then
    curl -fsSL https://get.docker.com | sudo sh
    sudo usermod -aG docker $USER
  fi
  if ! docker compose version >/dev/null 2>&1; then
    sudo apt-get update -y && sudo apt-get install -y docker-compose-plugin
  fi
  mkdir -p ~/n11
'

echo
echo "============================================================"
echo "Setup complete. GitHub repo Settings → Secrets and variables → Actions"
echo
echo "Repository VARIABLES:"
echo "  GCP_PROJECT_ID    = $GCP_PROJECT_ID"
echo "  GCP_REGION        = $GCP_REGION"
echo "  GCP_ZONE          = $GCP_ZONE"
echo "  GCP_AR_REPO       = $AR_REPO"
echo "  GCE_VM            = $VM_NAME"
echo
echo "Repository SECRETS:"
echo "  GCP_WORKLOAD_IDENTITY_PROVIDER = $WIF_PROVIDER_ID"
echo "  GCP_SERVICE_ACCOUNT            = $CI_SA_EMAIL"
echo "============================================================"
