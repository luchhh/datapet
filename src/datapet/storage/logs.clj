(ns datapet.storage.logs
  "Log storage and retrieval using Postgres.

   Provides functions for:
   - Inserting parsed logs
   - Querying logs with filters
   - Basic statistics"
  (:require [datapet.storage.db :as db]
            [honey.sql :as sql]
            [honey.sql.helpers :as h]
            [cheshire.core :as json]
            [clojure.tools.logging :as log])
  (:import [java.time Instant]
           [java.sql Timestamp]))

(defn insert-log!
  "Insert a parsed log entry into the database.

   Log should have: :timestamp, :service, :level, :message, :context"
  [{:keys [timestamp service level message context]}]
  (let [ts (Timestamp/from (Instant/parse timestamp))
        sql-map {:insert-into :logs
                 :columns [:timestamp :service :level :message :context]
                 :values [[ts
                           service
                           level
                           message
                           [:cast (json/generate-string (or context {})) :jsonb]]]}]
    (db/execute! (sql/format sql-map))))

(defn insert-logs!
  "Insert multiple log entries in batch"
  [logs]
  (doseq [log-entry logs]
    (insert-log! log-entry)))

(defn- build-base-query
  "Build a base query map with filters but no pagination or ordering."
  [{:keys [service level from to search]}]
  (cond-> {:select [:*]
           :from [:logs]}

    service
    (h/where [:= :service service])

    level
    (h/where [:= :level level])

    from
    (h/where [:>= :timestamp (Timestamp/from (Instant/parse from))])

    to
    (h/where [:<= :timestamp (Timestamp/from (Instant/parse to))])

    search
    (h/where [:ilike :message (str "%" search "%")])))

(defn build-query
  "Build a query map with optional filters.

   Options:
   - :service - filter by service name
   - :level - filter by log level
   - :from - start timestamp (ISO-8601 string)
   - :to - end timestamp (ISO-8601 string)
   - :search - text search in message
   - :limit - max results (default 100)
   - :offset - pagination offset"
  [{:keys [limit offset] :or {limit 100 offset 0} :as filters}]
  (-> (build-base-query filters)
      (assoc :order-by [[:timestamp :desc]]
             :limit limit
             :offset offset)))

(defn query-logs
  "Query logs with filters. Returns a vector of log maps."
  [filters]
  (db/query (sql/format (build-query filters))))

(defn get-log-by-id
  "Get a single log entry by ID"
  [id]
  (db/execute-one! (sql/format {:select [:*]
                                :from [:logs]
                                :where [:= :id id]})))

(defn get-stats
  "Get basic log statistics"
  []
  (let [total-sql "SELECT count(*) as total FROM logs"
        by-level-sql "SELECT level, count(*) as count FROM logs GROUP BY level"
        by-service-sql "SELECT service, count(*) as count FROM logs GROUP BY service ORDER BY count DESC LIMIT 10"]
    {:total (:total (db/execute-one! [total-sql]))
     :by-level (db/query [by-level-sql])
     :by-service (db/query [by-service-sql])}))

(defn count-logs
  "Count logs matching filters"
  [filters]
  (let [sql-map (-> (build-base-query filters)
                    (assoc :select [[[:count :*] :count]]))]
    (:count (db/execute-one! (sql/format sql-map)))))
