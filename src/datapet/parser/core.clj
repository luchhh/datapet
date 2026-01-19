(ns datapet.parser.core
  "Log parser consumer.

   Consumes raw logs from Kafka, parses/enriches them,
   and stores them in Postgres."
  (:require [datapet.kafka.consumer :as consumer]
            [datapet.storage.logs :as logs]
            [clojure.tools.logging :as log]))

;; Control atom for the parser consumer loop
(defonce parser-running (atom nil))

(defn validate-log
  "Validate a log entry has required fields"
  [{:keys [timestamp service level message] :as log-entry}]
  (and timestamp service level message))

(defn enrich-log
  "Enrich a log entry with additional metadata"
  [log-entry]
  (-> log-entry
      (update :context #(or % {}))
      (assoc-in [:context :parsed-at] (.toString (java.time.Instant/now)))))

(defn process-log!
  "Process a single log entry: validate, enrich, store"
  [log-entry]
  (if (validate-log log-entry)
    (do
      (log/debug "Processing log:" (:service log-entry) (:level log-entry))
      (-> log-entry
          enrich-log
          logs/insert-log!))
    (log/warn "Invalid log entry, skipping:" log-entry)))

(defn start!
  "Start the parser consumer"
  [config]
  (let [topic (get-in config [:kafka :topics :raw-logs])]
    (log/info "Starting parser consumer for topic:" topic)
    (reset! parser-running
            (consumer/start-consumer-loop!
             config
             topic
             {:handler process-log!
              :group-id-suffix "parser"}))))

(defn stop!
  "Stop the parser consumer"
  []
  (when @parser-running
    (log/info "Stopping parser consumer")
    (consumer/stop-consumer-loop! @parser-running)
    (reset! parser-running nil)))
