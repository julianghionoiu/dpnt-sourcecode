# dpnt-sourcecode
Datapoint processing - sourcecode


## Deploying

To deploy the function, execute

```
gradle build
serverless deploy
```

## Testing

Start external dependencies
```bash
python local-sqs/fetch-elasticmq-and-run.py start
python local-github/local-github-run.py start
python local-s3/minio-wrapper.py start
```

Run the acceptance test

```
./gradlew --rerun-tasks test jacocoTestReport
```

Start external dependencies
```bash
python local-sqs/fetch-elasticmq-and-run.py stop
python local-github/local-github-run.py stop
python local-s3/minio-wrapper.py stop
```
