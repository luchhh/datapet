(ns datapet.core
  "DataPet main entry point.

   Manages the lifecycle of all components:
   - Database connection pool
   - Kafka producer
   - Parser consumer
   - Simulator (optional)"
  (:require [datapet.config :as config]
            [datapet.storage.db :as db]
            [datapet.kafka.producer :as producer]
            [datapet.parser.core :as parser]
            [datapet.simulator.core :as simulator]
            [clojure.tools.logging :as log])
  (:gen-class))

(defonce system-config (atom nil))

(defn start-system!
  "Start all DataPet components"
  [config]
  (log/info "Starting DataPet...")
  (reset! system-config config)

  ;; Initialize database
  (log/info "Initializing database connection pool...")
  (db/init! (:postgres config))

  ;; Initialize Kafka producer
  (log/info "Initializing Kafka producer...")
  (producer/init! config)

  ;; Start parser consumer
  (log/info "Starting parser consumer...")
  (parser/start! config)

  ;; Start simulator if enabled
  (when (get-in config [:simulator :enabled])
    (log/info "Starting simulator...")
    (simulator/start! config))

  (log/info "DataPet started successfully!")
  (log/info "Logs will be stored in Postgres and can be queried via the API"))

(defn stop-system!
  "Stop all DataPet components"
  []
  (log/info "Stopping DataPet...")

  ;; Stop in reverse order
  (simulator/stop!)
  (parser/stop!)
  (producer/stop!)
  (db/stop!)

  (reset! system-config nil)
  (log/info "DataPet stopped"))

(defn add-shutdown-hook!
  "Register JVM shutdown hook for graceful shutdown"
  []
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. ^Runnable stop-system!)))

(defn -main
  "Main entry point"
  [& args]
  (log/info "=== DataPet Log Aggregation System ===")

  ;; Load configuration
  (let [config (config/load-config)]
    (log/info "Configuration loaded")

    ;; Register shutdown hook
    (add-shutdown-hook!)

    ;; Start the system
    (start-system! config)

    ;; Keep main thread alive
    (log/info "Press Ctrl+C to stop")
    @(promise)))
