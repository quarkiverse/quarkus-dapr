# How to run quarkus-dapr demo

## preparation

Before running this demo, please ensure that you have the following software installed:

- jdk 11: GraalVM
- maven 3.8.*
- quarkus 2.8.0
- docker
- dapr cli

### dapr install

```bash

dapr init

```

## Build Native Binary

```bash
# clone code
git clone https://github.com/quarkiverse/quarkus-dapr.git

# maven build
cd quarkus-dapr
mvn clean install

# build native binary for app1
cd demo
cd app1
quarkus build --native
# ensure app1 binary file exists
ls -lh target/quarkus-dapr-demo-app1-1.0.0-SNAPSHOT-runner

# build native binary for app2
cd ..
cd app2
quarkus build --native
# ensure app2 binary file exists
ls -lh target/quarkus-dapr-demo-app2-1.0.0-SNAPSHOT-runner
cd ..
```

## Start Redis 

In this demo, we will use redis as component for pubsub / state . 

if you just installed Dapr runtime by `dapr init` command, then redis should already run with docker:

```bash
# check if redis is running with docker
docker ps
# verify redis 6379 port
nc  -zv  127.0.0.1 6379
```

if redis is not running, please start it with docker command:

```bash
# start redis by docker
docker run -d -p 6379:6379 redis --requirepass ""
# verify that redis is listening on 6379 port
nc  -zv  127.0.0.1 6379
```

## Start Demo App

### start app1

Start app1 in your terminal, you can start app1 with dapr runtime by dapr CLI:

```bash
# start app1 with dapr runtime
cd app1
dapr run --app-port 8081 --app-id app1 --app-protocol http --dapr-http-port 3501 --dapr-grpc-port 50001 -- target/quarkus-dapr-demo-app1-1.0.0-SNAPSHOT-runner
```

Or start dapr runtime and app1 one by one:

```bash
# start dapr runtime without app1
dapr run --app-port 8081 --app-id app1 --app-protocol http --dapr-http-port 3501 --dapr-grpc-port 50001 --components-path=./components

# start app1 in another terminal
cd quarkus-dapr/demo/app1
./target/quarkus-dapr-demo-app1-1.0.0-SNAPSHOT-runner
```

In this way, we can run DaprDemoApplication in IDE to quickly development without build native image, or debugging in IDE.

Check the log to see if app1 and dapr runtime start successfully. Open your browser and access http://localhost:8081/pubsub/ 

### start app2

Start app2 in your terminal:

```bash
# start app2 with dapr runtime
cd app2
dapr run --app-port 3002 --app-id app2 --app-protocol http --dapr-http-port 3502 --dapr-grpc-port 50002 -- target/quarkus-dapr-demo-app2-1.0.0-SNAPSHOT-runner
```

Or start dapr runtime and app2 one by one:

```bash
# start dapr runtime without app2
dapr run --app-port 8082 --app-id app2 --app-protocol http --dapr-http-port 3502 --dapr-grpc-port 50002 --components-path=./components

# start app2 in another terminal
cd quarkus-dapr/demo/app2
./target/quarkus-dapr-demo-app2-1.0.0-SNAPSHOT-runner
```

Check the log to see if app2 and dapr runtime start successfully.

## demo

### pubsub demo

This demo will show how to publish event and subscribe event by Dapr pubsub building block.

1. start app1 and app2
2. Trigger app1 to send an event to topic1 by visiting url http://localhost:8081/pubsub/trigger/topic1 
3. App2 has subscribed on topic1, so app2 should receive an event from topic1
4. Then app2 will publish an event to topic2 
5. App1 has subscribed on topic2, so app1 should receive an event from topic2