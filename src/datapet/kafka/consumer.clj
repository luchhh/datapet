(ns datapet.kafka.consumer
  "Kafka consumer utilities.

   Provides base functionality for creating consumers and processing records.
   Specific consumers (parser, aggregator) build on top of this."
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log])
  (:import [org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords]
           [org.apache.kafka.common.serialization StringDeserializer]
           [java.time Duration]))

(defn create-consumer
  "Create a Kafka consumer with the given config"
  [bootstrap-servers group-id]
  (let [config {"bootstrap.servers" bootstrap-servers
                "group.id" group-id
                "key.deserializer" (.getName StringDeserializer)
                "value.deserializer" (.getName StringDeserializer)
                "auto.offset.reset" "earliest"
                "enable.auto.commit" "true"}]
    (log/info "Creating Kafka consumer" group-id "for" bootstrap-servers)
    (KafkaConsumer. config)))

(defn subscribe!
  "Subscribe consumer to topics"
  [^KafkaConsumer consumer topics]
  (let [topic-list (if (sequential? topics) topics [topics])]
    (log/info "Subscribing to topics:" topic-list)
    (.subscribe consumer (java.util.ArrayList. topic-list))))

(defn poll-records
  "Poll for records with timeout in ms"
  [^KafkaConsumer consumer timeout-ms]
  (.poll consumer (Duration/ofMillis timeout-ms)))

(defn parse-record
  "Parse a Kafka record value as JSON"
  [record]
  (try
    (json/parse-string (.value record) true)
    (catch Exception e
      (log/warn "Failed to parse record:" (.getMessage e))
      nil)))

(defn process-records
  "Process consumer records with a handler function.
   Handler receives each parsed record."
  [^ConsumerRecords records handler]
  (doseq [record records]
    (when-let [parsed (parse-record record)]
      (try
        (handler parsed)
        (catch Exception e
          (log/error e "Error processing record:" parsed))))))

(defn start-consumer-loop!
  "Start a consumer loop that polls and processes records.

   Returns an atom that can be set to false to stop the loop.
   The loop runs in a separate thread.

   Options:
   - :poll-timeout-ms - poll timeout (default 1000)
   - :handler - function to call for each record"
  [config topics {:keys [poll-timeout-ms handler group-id-suffix]
                  :or {poll-timeout-ms 1000}}]
  (let [running (atom true)
        bootstrap-servers (get-in config [:kafka :bootstrap-servers])
        base-group-id (get-in config [:kafka :group-id])
        group-id (if group-id-suffix
                   (str base-group-id "-" group-id-suffix)
                   base-group-id)
        consumer (create-consumer bootstrap-servers group-id)]

    ;; Start processing in a new thread
    (future
      (try
        (subscribe! consumer topics)
        (log/info "Consumer loop started for" topics)
        (while @running
          (let [records (poll-records consumer poll-timeout-ms)]
            (when-not (.isEmpty records)
              (process-records records handler))))
        (catch Exception e
          (log/error e "Consumer loop error"))
        (finally
          (log/info "Consumer loop stopping for" topics)
          (.close consumer))))

    ;; Return the control atom
    running))

(defn stop-consumer-loop!
  "Stop a consumer loop by setting its control atom to false"
  [running-atom]
  (reset! running-atom false))
