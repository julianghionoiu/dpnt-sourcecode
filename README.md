# dpnt-sourcecode
Datapoint processing - sourcecode

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

Stop external dependencies
```bash
python local-sqs/elasticmq-wrapper.py stop
python local-github/local-github-run.py stop
python local-s3/minio-wrapper.py stop
```

## Packaging

Install Serverless
```
npm install -g serverless

serverless info
```

Now, have a look at `serverless.yml`

Create an environment configuration in `./config` by creating copy after `env.local.yml`

## Local testing

Build package
```
./gradlew clean test shadowJar
```

Invoke function manually
```
SLS_DEBUG=* serverless invoke local --function srcs-github-export --path tdl/dpnt-sourcecode/src/test/resources/tdl/datapoint/sourcecode/sample_s3_event.json
```

## Remote deployment

Obtain the Github access token by following this tutorial:
https://developer.github.com/apps/building-github-apps/authentication-options-for-github-apps/

Build package
```
./gradlew clean test shadowJar
```

Deploy to DEV
```
serverless deploy --stage dev
```

Deploy to LIVE
```
serverless deploy --stage live
```

## Remote testing

Create an S3 event json and place it in a temp folder, say `xyz/s3_event.json`
Set the bucket and the key to some meaningful values.

Invoke the dev lambda

```
SLS_DEBUG=* serverless invoke --stage dev --function srcs-github-export --path src/test/resources/tdl/datapoint/sourcecode/sample_s3_event.json
```
