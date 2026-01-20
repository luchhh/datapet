(ns datapet.api.routes
  "API routes and handlers using Reitit."
  (:require [reitit.ring :as ring]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [datapet.storage.logs :as logs]
            [cheshire.core :as json]
            [clojure.tools.logging :as log])
  (:import [org.postgresql.util PGobject]))

(defn- parse-int
  "Parse string to integer, returning default if nil or invalid."
  [s default]
  (if (some? s)
    (try
      (Integer/parseInt s)
      (catch NumberFormatException _ default))
    default))

(defn- transform-log
  "Transform a log entry for JSON serialization.
   Converts PGobject (JSONB) to Clojure map."
  [log-entry]
  (if-let [ctx (:context log-entry)]
    (assoc log-entry :context
           (if (instance? PGobject ctx)
             (json/parse-string (.getValue ctx) true)
             ctx))
    log-entry))

(defn- parse-filters
  "Parse query params into filter map for logs query."
  [params]
  (let [{:strs [service level from to search limit offset]} params]
    (cond-> {}
      service (assoc :service service)
      level (assoc :level level)
      from (assoc :from from)
      to (assoc :to to)
      search (assoc :search search)
      true (assoc :limit (parse-int limit 100))
      true (assoc :offset (parse-int offset 0)))))

(defn list-logs
  "Handler for GET /api/logs - list logs with filters."
  [request]
  (try
    (let [filters (parse-filters (:query-params request))
          data (mapv transform-log (logs/query-logs filters))
          count (logs/count-logs filters)]
      {:status 200
       :body {:data data
              :count count}})
    (catch Exception e
      (log/error e "Error listing logs")
      {:status 500
       :body {:error "Internal server error"}})))

(defn get-log
  "Handler for GET /api/logs/:id - get single log by ID."
  [request]
  (try
    (let [id (parse-int (get-in request [:path-params :id]) nil)]
      (if (nil? id)
        {:status 400
         :body {:error "Invalid log ID"}}
        (if-let [log-entry (logs/get-log-by-id id)]
          {:status 200
           :body {:data (transform-log log-entry)}}
          {:status 404
           :body {:error "Log not found"}})))
    (catch Exception e
      (log/error e "Error getting log")
      {:status 500
       :body {:error "Internal server error"}})))

(defn get-stats
  "Handler for GET /api/stats - basic log statistics."
  [_request]
  (try
    {:status 200
     :body {:data (logs/get-stats)}}
    (catch Exception e
      (log/error e "Error getting stats")
      {:status 500
       :body {:error "Internal server error"}})))

(def routes
  "API route definitions."
  ["/api"
   ["/logs" {:get list-logs}]
   ["/logs/:id" {:get get-log}]
   ["/stats" {:get get-stats}]])

(defn create-app
  "Create the Ring application with middleware."
  []
  (-> (ring/ring-handler
       (ring/router routes)
       (ring/create-default-handler
        {:not-found (constantly {:status 404
                                 :body {:error "Not found"}})}))
      wrap-params
      wrap-json-response))
