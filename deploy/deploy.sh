#!/usr/bin/env bash
# Deploys the application on the web server. Run as root on the server:
#
#   IMAGE_TAG=abc1234 REF=main bash deploy.sh
#
# IMAGE_TAG  tag of the images pushed to ECR (required)
# REF        Git branch or tag the compose file is downloaded from (default: main)
set -euo pipefail

REGION="${REGION:-ap-south-1}"
PROJECT="${PROJECT:-url-shortener}"
REPO="${REPO:-RamlaHussain458/url-shortener-aws}"
REF="${REF:-main}"
IMAGE_TAG="${IMAGE_TAG:?Set IMAGE_TAG to the image tag you want to deploy}"
APP_DIR="/opt/${PROJECT}"

get_param() {
  aws ssm get-parameter --region "$REGION" --name "/${PROJECT}/db/$1" \
    --with-decryption --query Parameter.Value --output text
}

mkdir -p "$APP_DIR"
cd "$APP_DIR"

echo "Downloading docker-compose.prod.yml from ${REPO} (${REF})"
curl -fsSL "https://raw.githubusercontent.com/${REPO}/${REF}/deploy/docker-compose.prod.yml" \
  -o docker-compose.prod.yml

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

# The server asks its own metadata service for its public address (IMDSv2).
IMDS_TOKEN="$(curl -fsS -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 60")"
PUBLIC_IP="$(curl -fsS -H "X-aws-ec2-metadata-token: ${IMDS_TOKEN}" \
  "http://169.254.169.254/latest/meta-data/public-ipv4")"

echo "Writing configuration (the file is readable by root only)"
umask 077
{
  echo "DB_HOST=$(get_param host)"
  echo "DB_NAME=$(get_param name)"
  echo "DB_USER=$(get_param user)"
  echo "DB_PASSWORD=$(get_param password)"
  echo "ECR_REGISTRY=${REGISTRY}"
  echo "IMAGE_TAG=${IMAGE_TAG}"
  echo "PUBLIC_BASE_URL=http://${PUBLIC_IP}"
} > .env

COMPOSE=(docker compose -f docker-compose.prod.yml --env-file .env)

echo "Pulling images tagged ${IMAGE_TAG}"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"
"${COMPOSE[@]}" pull
"${COMPOSE[@]}" up -d --remove-orphans
docker logout "$REGISTRY"
docker image prune -f

echo "Waiting for the application to become healthy"
for _ in $(seq 1 40); do
  health="$(curl -fsS http://127.0.0.1:8080/actuator/health || true)"
  if [[ "$health" == *'"status":"UP"'* ]]; then
    echo "Deployed ${IMAGE_TAG}. Open http://${PUBLIC_IP}"
    exit 0
  fi
  sleep 5
done

echo "The application did not become healthy in time. Recent logs:" >&2
"${COMPOSE[@]}" logs --tail 50 app >&2
exit 1
