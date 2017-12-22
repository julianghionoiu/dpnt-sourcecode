#!/usr/bin/env bash
wget -nc https://dl.minio.io/server/minio/release/linux-amd64/minio
chmod +x minio
mkdir -p ./tmp/minio
./minio server ./tmp/minio
