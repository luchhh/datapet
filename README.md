# Kafka Pipelines - Clojure

A learning project exploring Kafka streaming pipelines with Clojure.

## Prerequisites

- [Clojure CLI tools](https://clojure.org/guides/install_clojure)
- [Docker](https://www.docker.com/get-started) and Docker Compose
- Java 11 or higher

## Project Structure

```
pipelines/
├── docker-compose.yml       # Kafka + Zookeeper setup
└── src/
    └── pipelines/
        ├── deps.edn         # Project dependencies
        └── consumer.clj     # Kafka consumer implementation
```

## Getting Started

### 1. Start Kafka Infrastructure

Start Zookeeper and Kafka using Docker Compose:

```bash
docker-compose up -d
```

Verify containers are running:

```bash
docker ps
```

You should see `kafka` and `zookeeper` containers running.

### 2. Create a Test Topic

Create the `test-events` topic:

```bash
docker exec -it kafka kafka-topics --create \
  --topic test-events \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1
```

List topics to verify:

```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 3. Run the Consumer

Navigate to the source directory and start the consumer:

```bash
cd src/pipelines
clj -M -m pipelines.consumer
```

You should see:
```
✅ Listening to topic: test-events
```

### 4. Send Test Messages

In a separate terminal, produce messages to test the consumer:

```bash
docker exec -it kafka kafka-console-producer \
  --topic test-events \
  --bootstrap-server localhost:9092
```

Type messages and press Enter. You'll see them appear in your consumer terminal:

```
📨 Received: Hello Kafka!
📨 Received: Testing pipelines
```

## What's Implemented

- **Kafka Consumer**: Subscribes to `test-events` topic and prints received messages
- **Consumer Group**: Uses `clojure-consumer` as the consumer group ID
- **Auto Offset Reset**: Configured to read from earliest available messages

## Cleanup

Stop the consumer with `Ctrl+C`, then stop Kafka infrastructure:

```bash
docker-compose down
```

To remove volumes (deletes all data):

```bash
docker-compose down -v
```

## Learning Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Clojure CLI Guide](https://clojure.org/guides/deps_and_cli)
- [Kafka Clients Javadoc](https://kafka.apache.org/36/javadoc/index.html)

## Next Steps

Ideas for expanding this project:

- Add a Kafka producer in Clojure
- Implement message processing pipelines
- Explore Kafka Streams
- Add error handling and logging
- Create multiple consumers with different group IDs
- Experiment with partitioning and parallel processing
