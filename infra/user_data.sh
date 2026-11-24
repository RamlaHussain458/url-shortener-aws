#!/bin/bash
# Runs once, as root, the first time the server boots. Log: /var/log/user-data.log
set -euxo pipefail
exec > >(tee /var/log/user-data.log) 2>&1

dnf install -y docker
systemctl enable --now docker

# Docker Compose v2 plugin
mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# A small swap file as a safety net against memory spikes
if [ ! -f /swapfile ]; then
  dd if=/dev/zero of=/swapfile bs=1M count=1024
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

mkdir -p /opt/url-shortener
echo "bootstrap finished" > /opt/url-shortener/.bootstrap-done
