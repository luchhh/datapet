(ns datapet.simulator.core
  "Fake log generator for testing and demos.

   Generates realistic-looking logs from multiple simulated services."
  (:require [datapet.kafka.producer :as producer]
            [clojure.tools.logging :as log])
  (:import [java.util Random]))

;; Control atom for the simulator loop
(defonce simulator-running (atom nil))

(def ^:private random (Random.))

(def log-templates
  "Templates for generating realistic log messages by service"
  {"auth-api"
   {:info ["User logged in successfully"
           "Session created"
           "Token refreshed"
           "Password reset email sent"]
    :warn ["Failed login attempt"
           "Session expired"
           "Rate limit approaching"]
    :error ["Authentication failed"
            "Invalid token"
            "Database connection timeout"
            "Redis connection lost"]}

   "payment-service"
   {:info ["Payment processed successfully"
           "Refund initiated"
           "Invoice generated"
           "Subscription renewed"]
    :warn ["Payment retry scheduled"
           "Slow payment gateway response"
           "Currency conversion delay"]
    :error ["Payment failed"
            "Gateway timeout"
            "Insufficient funds"
            "Card declined"]}

   "user-service"
   {:info ["User profile updated"
           "Email preferences changed"
           "Account created"
           "Avatar uploaded"]
    :warn ["Profile validation warning"
           "Duplicate email detected"
           "Large file upload"]
    :error ["Profile update failed"
            "Email already exists"
            "Invalid user data"
            "Storage quota exceeded"]}

   "gateway"
   {:info ["Request routed successfully"
           "Cache hit"
           "Health check passed"
           "Connection established"]
    :warn ["Slow upstream response"
           "Cache miss"
           "Retry triggered"
           "Connection pool low"]
    :error ["Upstream timeout"
            "Service unavailable"
            "Circuit breaker open"
            "Request rejected"]}})

(defn random-element
  "Pick a random element from a collection"
  [coll]
  (nth coll (.nextInt random (count coll))))

(defn random-level
  "Generate a random log level with realistic distribution"
  []
  (let [n (.nextInt random 100)]
    (cond
      (< n 70) :info    ; 70% info
      (< n 90) :warn    ; 20% warn
      :else :error)))   ; 10% error

(defn generate-context
  "Generate random context data for a log entry"
  [service]
  (let [base {:request-id (str (java.util.UUID/randomUUID))
              :host (str service "-" (inc (.nextInt random 5)))}]
    (case service
      "auth-api" (assoc base :user-id (+ 1000 (.nextInt random 9000)))
      "payment-service" (assoc base
                               :amount (+ 10 (.nextInt random 500))
                               :currency "USD")
      "user-service" (assoc base :user-id (+ 1000 (.nextInt random 9000)))
      "gateway" (assoc base
                       :upstream service
                       :latency-ms (+ 10 (.nextInt random 200)))
      base)))

(defn generate-log
  "Generate a random log entry for a service"
  [service]
  (let [level (random-level)
        templates (get-in log-templates [service level])
        message (if templates
                  (random-element templates)
                  (str "Log from " service))]
    {:timestamp (.toString (java.time.Instant/now))
     :service service
     :level (name level)
     :message message
     :context (generate-context service)}))

(defn emit-log!
  "Generate and emit a random log"
  [config services]
  (let [service (random-element services)
        log-entry (generate-log service)
        topic (get-in config [:kafka :topics :raw-logs])]
    (log/debug "Emitting log:" (:service log-entry) (:level log-entry))
    (producer/send-log! topic log-entry)))

(defn start!
  "Start the simulator loop"
  [config]
  (let [interval-ms (get-in config [:simulator :interval-ms] 1000)
        services (get-in config [:simulator :services])
        running (atom true)]
    (log/info "Starting simulator with interval" interval-ms "ms")
    (log/info "Simulating services:" services)

    (future
      (try
        (while @running
          (emit-log! config services)
          (Thread/sleep interval-ms))
        (catch Exception e
          (log/error e "Simulator error"))
        (finally
          (log/info "Simulator stopped"))))

    (reset! simulator-running running)))

(defn stop!
  "Stop the simulator"
  []
  (when @simulator-running
    (log/info "Stopping simulator")
    (reset! @simulator-running false)
    (reset! simulator-running nil)))
