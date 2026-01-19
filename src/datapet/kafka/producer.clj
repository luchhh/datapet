(ns datapet.kafka.producer
  "Kafka producer for emitting log events.

   Provides a simple API for services to emit structured logs to Kafka.
   Logs are serialized as JSON and sent to the raw logs topic."
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log])
  (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
           [org.apache.kafka.common.serialization StringSerializer]))

;; Global producer atom
(defonce producer (atom nil))

(defn create-producer
  "Create a Kafka producer with the given bootstrap servers"
  [bootstrap-servers]
  (let [config {"bootstrap.servers" bootstrap-servers
                "key.serializer" (.getName StringSerializer)
                "value.serializer" (.getName StringSerializer)
                "acks" "all"
                "retries" (int 3)}]
    (log/info "Creating Kafka producer for" bootstrap-servers)
    (KafkaProducer. config)))

(defn init!
  "Initialize the global producer"
  [config]
  (when @producer
    (log/warn "Producer already initialized, closing existing")
    (.close ^KafkaProducer @producer))
  (reset! producer (create-producer (get-in config [:kafka :bootstrap-servers])))
  (log/info "Kafka producer initialized"))

(defn stop!
  "Shut down the producer"
  []
  (when-let [p @producer]
    (log/info "Shutting down Kafka producer")
    (.close ^KafkaProducer p)
    (reset! producer nil)))

(defn get-producer
  "Get the current producer, throwing if not initialized"
  []
  (or @producer
      (throw (ex-info "Producer not initialized. Call init! first." {}))))

(defn send-log!
  "Send a log entry to Kafka.

   Log entry should be a map with:
   - :timestamp - ISO-8601 timestamp string
   - :service   - service name (string)
   - :level     - log level (info, warn, error, debug)
   - :message   - log message
   - :context   - optional map of additional context"
  [topic log-entry]
  (let [key (:service log-entry)
        value (json/generate-string log-entry)
        record (ProducerRecord. topic key value)]
    (.send (get-producer) record)))

(defn emit!
  "Convenience function to emit a log entry to the raw logs topic.

   Usage:
   (emit! config \"auth-api\" :error \"Login failed\" {:user-id 123})"
  [config service level message & [context]]
  (let [topic (get-in config [:kafka :topics :raw-logs])
        entry {:timestamp (.toString (java.time.Instant/now))
               :service service
               :level (name level)
               :message message
               :context (or context {})}]
    (send-log! topic entry)))
