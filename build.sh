#!/usr/bin/env bash
if [ ! -f minio ]; then
    echo "Downloading minio"
    wget https://dl.minio.io/server/minio/release/linux-amd64/minio
    chmod +x minio
fi
echo "Starting minio"
export MINIO_ACCESS_KEY=minio_access_key
export MINIO_SECRET_KEY=minio_secret_key
export MINIO_BROWSER=off
mkdir -p tmp
./minio server tmp &
sleep 3

echo "Starting git repo"
CUR_DIR=`pwd`
TMPDIR=`mktemp -d`
DIR=$TMPDIR
git init $TMPDIR
cd $TMPDIR
echo "#README" > README.md
git add .
git commit -m "Initialize"
git checkout -b test #Enable client to push to master
git daemon \
    --reuseaddr \
    --export-all \
    --base-path=$TMPDIR \
    --enable=receive-pack \
    --port=1234 &
sleep 3
echo $CUR_DIR
cd $CUR_DIR

echo "Running test"
./gradlew --rerun-tasks test jacocoTestReport --info

echo "Killing server"
ps ax | grep minio | awk '{print $1}' | head -n 1 | xargs kill
ps ax | grep git-daemon | awk '{print $1}' | head -n 1 | xargs kill
sleep 3
rm -rf $TMPDIR
