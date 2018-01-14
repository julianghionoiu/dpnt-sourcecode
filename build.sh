#!/usr/bin/env bash

function run_minio() {
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
}

function run_git_repo() {
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
}

function run_elastic_mq() {
    if [ ! -f elasticmq-server.jar ]; then
        echo "Downloading elasticmq-server"
        wget -O elasticmq-server.jar https://s3-eu-west-1.amazonaws.com/softwaremill-public/elasticmq-server-0.13.8.jar
    fi
    echo "Starting elasticmq-server"
    java -jar elasticmq-server.jar &
    sleep 3
}

function run_github_local() {
    echo "Running github"
    CUR_DIR=`pwd`
    TMPDIR=`mktemp -d`
    DIR=$TMPDIR
    echo $TMPDIR > tmp/github-dir.txt
    python local-github/github-server.py $TMPDIR &
}

function run_test() {
    echo "Running test"
    ./gradlew --rerun-tasks test jacocoTestReport
}

function clean_up() {
    echo "Killing server"
    ps ax | grep minio | awk '{print $1}' | head -n 1 | xargs kill
    ps ax | grep git-daemon | awk '{print $1}' | head -n 1 | xargs kill
    ps ax | grep elasticmq-server | awk '{print $1}' | head -n 1 | xargs kill
    ps ax | grep elasticmq-server | awk '{print $1}' | head -n 1 | xargs kill
    ps ax | grep local-github | awk '{print $1}' | head -n 1 | xargs kill
    sleep 3
    rm -rf $TMPDIR
    rm -rf tmp/github-dir.txt
}

run_minio
run_git_repo
run_elastic_mq
run_github_local

run_test

clean_up
