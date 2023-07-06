# What is this?

Framework independent implementation of the core functionality which this product provides.



----


A library which embeds waiting room functionality into your components.

# Features

- delay until
- ttl
- etc...

# How it works

This module is deployed as part of your component. As such, it load balances like any Kafka Consumer.
Because it is deployed with your component, it can add waiting room functionality to any topics that 
your component subscribes to, without blocking other components subscribing to the same topics with other
consumer group IDs.

It uses the twr-outbox to send events back to itself, via Kafka, which are used to control
the contents of the waiting room, e.g. making a retry wait until a given time, or removing a sucessfully processed
record from the waiting room.

This module subscribes to records from the twr-inbox

# Useful stuff

- List test containers:


    docker ps --all --filter label=org.testcontainers=true --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Labels}}"
    docker ps --all --filter label=org.testcontainers=true --format "table {{.ID}}\t{{.Image}}\t{{.Status}}"

- Kill and remove all test containers:


    docker ps --filter label=org.testcontainers=true -aq | xargs docker kill
    docker rm -f $(docker ps --all --filter label=org.testcontainers=true -q)

    or:

    docker ps --filter label=org.testcontainers=true -aq | xargs docker kill | xargs docker rm

# TODO

- so does it contain an inbox or an outbox table, or both?!  or do we call it a twr-tasks table?
  - call it a tasks table
  - document that design choice
- rebalance listener on the offsets topic which has a ton of partitions
- why cant redpandaconsole actually read topics? maybe setup redpanda as we do in abstratium
- mapping which can be overriden
- publish to topic
- consume from twr topic and add our own waiting room
  - cant use PC?
    - how do we do things like replay? -> from source database and duplicate!! row, because effectively, it is a retry of something that already happened???
    - pause indefinitely?
- do we want to put everything under the same source control?
- document all "TWR logs and exceptions
- 