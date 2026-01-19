(ns datapet.config
  "Configuration loading and defaults.

   Configuration is loaded from environment variables with sensible defaults
   for local development. In production, override via environment."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def defaults
  "Default configuration for local development"
  {:kafka {:bootstrap-servers "localhost:9092"
           :group-id "datapet"
           :topics {:raw-logs "logs.raw"
                    :parsed-logs "logs.parsed"
                    :alerts "alerts.triggered"}}

   :postgres {:host "localhost"
              :port 5433
              :database "datapet"
              :user "datapet"
              :password "datapet"}

   :http {:port 3000}

   :simulator {:enabled true
               :interval-ms 1000
               :services ["auth-api" "payment-service" "user-service" "gateway"]}})

(defn get-env
  "Get environment variable with optional default"
  ([key] (get-env key nil))
  ([key default] (or (System/getenv key) default)))

(defn load-config
  "Load configuration, merging environment overrides with defaults"
  []
  (-> defaults
      (assoc-in [:kafka :bootstrap-servers]
                (get-env "KAFKA_BOOTSTRAP_SERVERS" "localhost:9092"))
      (assoc-in [:postgres :host]
                (get-env "POSTGRES_HOST" "localhost"))
      (assoc-in [:postgres :port]
                (Integer/parseInt (get-env "POSTGRES_PORT" "5433")))
      (assoc-in [:postgres :database]
                (get-env "POSTGRES_DB" "datapet"))
      (assoc-in [:postgres :user]
                (get-env "POSTGRES_USER" "datapet"))
      (assoc-in [:postgres :password]
                (get-env "POSTGRES_PASSWORD" "datapet"))
      (assoc-in [:http :port]
                (Integer/parseInt (get-env "HTTP_PORT" "3000")))))
