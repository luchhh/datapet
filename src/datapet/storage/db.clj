(ns datapet.storage.db
  "Database connection pool management using HikariCP.

   This module provides:
   - Connection pool creation/shutdown
   - A datasource atom for global access
   - Helper functions for executing queries"
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.tools.logging :as log])
  (:import [com.zaxxer.hikari HikariConfig HikariDataSource]))

;; Global datasource atom - set during initialization
(defonce datasource (atom nil))

(defn create-pool
  "Create a HikariCP connection pool from config"
  [{:keys [host port database user password]}]
  (let [jdbc-url (format "jdbc:postgresql://%s:%d/%s" host port database)
        config (doto (HikariConfig.)
                 (.setJdbcUrl jdbc-url)
                 (.setUsername user)
                 (.setPassword password)
                 (.setMaximumPoolSize 10)
                 (.setMinimumIdle 2)
                 (.setConnectionTimeout 30000)
                 (.setIdleTimeout 600000)
                 (.setMaxLifetime 1800000)
                 (.setPoolName "datapet-pool"))]
    (log/info "Creating connection pool for" jdbc-url)
    (HikariDataSource. config)))

(defn init!
  "Initialize the global datasource with config"
  [config]
  (when @datasource
    (log/warn "Datasource already initialized, closing existing pool")
    (.close ^HikariDataSource @datasource))
  (reset! datasource (create-pool config))
  (log/info "Database connection pool initialized"))

(defn stop!
  "Shut down the connection pool"
  []
  (when-let [ds @datasource]
    (log/info "Shutting down database connection pool")
    (.close ^HikariDataSource ds)
    (reset! datasource nil)))

(defn get-datasource
  "Get the current datasource, throwing if not initialized"
  []
  (or @datasource
      (throw (ex-info "Database not initialized. Call init! first." {}))))

(defn execute!
  "Execute a SQL statement (insert, update, delete)"
  [sql-params]
  (jdbc/execute! (get-datasource) sql-params))

(defn execute-one!
  "Execute a SQL statement and return first result"
  [sql-params]
  (jdbc/execute-one! (get-datasource) sql-params
                     {:builder-fn rs/as-unqualified-lower-maps}))

(defn query
  "Execute a query and return all results as maps"
  [sql-params]
  (jdbc/execute! (get-datasource) sql-params
                 {:builder-fn rs/as-unqualified-lower-maps}))
