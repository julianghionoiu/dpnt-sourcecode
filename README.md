# dpnt-sourcecode
Datapoint processing - sourcecode

## Setting up Serverless

```
npm install -g serverless
npm install -g serverless-plugin-existing-s3

serverless info
```



## Acceptance test

Start external dependencies
```bash
python local-sqs/elasticmq-wrapper.py start
python local-github/github-server-wrapper.py start
python local-s3/minio-wrapper.py start
```

Run the acceptance test

```
./gradlew --rerun-tasks test jacocoTestReport
```


## Packaging

Have a look at `serverless.yml`

Create an environment configuration in `./config` by creating copy after `config.reference.yml.template`

## Local testing

Build package
```
./gradlew clean build
```

Invoke function manually
```
serverless invoke local --function srcs-github-export
```


## Clean up

Start external dependencies
```bash
python local-sqs/fetch-elasticmq-and-run.py stop
python local-github/local-github-run.py stop
python local-s3/minio-wrapper.py stop
```