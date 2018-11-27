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
./gradlew --rerun-tasks test
```

Stop external dependencies
```bash
python local-sqs/elasticmq-wrapper.py stop
python local-github/github-server-wrapper.py stop
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

Create a Minio bucket locally
```
export TEST_S3_LOCATION=./local-s3/.storage/tdl-test-auth/TCH/user01
mkdir -p $TEST_S3_LOCATION
cp src/test/resources/test1.srcs $TEST_S3_LOCATION
echo $TEST_S3_LOCATION && ls -l $TEST_S3_LOCATION
```

Invoke local function manually
```
AWS_ACCESS_KEY_ID=local_test_access_key \
AWS_SECRET_KEY=local_test_secret_key \
SLS_DEBUG=* \
serverless invoke local \
 --function srcs-github-export \
 --path src/test/resources/tdl/datapoint/sourcecode/sample_s3_event.json
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
SLS_DEBUG=* serverless invoke --stage dev --function srcs-github-export --path src/test/resources/tdl/datapoint/sourcecode/sample_s3_via_sns_event.json
```
