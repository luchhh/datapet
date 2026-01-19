(ns pipelines.consumer
  (:import [org.apache.kafka.clients.consumer KafkaConsumer ConsumerRecords]
           [org.apache.kafka.common.serialization StringDeserializer])
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn make-consumer
  []
  (KafkaConsumer.
   {"bootstrap.servers" "localhost:9092"
    "group.id" "clojure-consumer"
    "key.deserializer" StringDeserializer
    "value.deserializer" StringDeserializer
    "auto.offset.reset" "earliest"}))

(defn start-consumer
  []
  (with-open [consumer (make-consumer)]
    (.subscribe consumer (java.util.Collections/singletonList "test-events"))
    (println "✅ Listening to topic: test-events")
    (loop []
      (let [records (.poll consumer 1000)]
        (doseq [record records]
          (println "📨 Received:" (.value record))))
      (recur))))

(defn -main
  [& args]
  (start-consumer))
