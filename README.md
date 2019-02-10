# dpnt-sourcecode
Datapoint processing - sourcecode

### Updating sub-modules

Root project contains three git submodules:

- local-github  
- local-s3  
- local-sqs 

Run the below command in the project root to update the above submodules:

```
git submodule update --init
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
./gradlew --rerun-tasks test
```

Stop external dependencies
```bash
python local-sqs/elasticmq-wrapper.py stop
python local-github/github-server-wrapper.py stop
python local-s3/minio-wrapper.py stop
minio config host add myminio http://192.168.1.190:9000 local_test_access_key local_test_secret_key

```

## Packaging

Install Serverless

Ensure you have new version (v6.4.0) of `npm` installed, installing `serverless` fails with older versions of npm:

```
npm install -g npm         # optional: to get the latest version of npm
npm install -g serverless

serverless info
```

Now, have a look at `serverless.yml`

Create an environment configuration in `./config` by creating copy after `env.local.yml`

## Local testing

Ensure that the three components above i.e. `local-xxx` are running before running the below commands.

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

Create config file for respective env profiles:

```
cp config/local.params.yml config/dev.params.yml
```

or

```
cp config/dev.params.yml config/live.params.yml
```


Invoke local function manually
```
AWS_ACCESS_KEY_ID=local_test_access_key \
AWS_SECRET_KEY=local_test_secret_key \
SLS_DEBUG=* \
serverless invoke local \
 --function srcs-github-export \
 --path src/test/resources/tdl/datapoint/sourcecode/sample_s3_via_sns_event.json
```

Note: the below can also be used:

```
export AWS_PROFILE=befaster                        # pre-configured profile contained in ~/.aws/credentials
```

instead of setting `AWS_ACCESS_KEY_ID` and `AWS_SECRET_KEY`


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

Check the destination queue for that particular environment. Check the ECS Task status and logs

Note: the sample_s3_via_sns_event.json file contains the reference to the bucket tdl-test-auth and the key referring to the file at TCH/user01/test1.srcs.