(ns datapet.api.server
  "HTTP server lifecycle using Ring + Jetty."
  (:require [ring.adapter.jetty :as jetty]
            [clojure.tools.logging :as log]))

(defonce server (atom nil))

(defn start!
  "Start the Jetty HTTP server.

   config - map containing :http {:port ...}
   app - Ring handler function"
  [config app]
  (let [port (get-in config [:http :port] 3000)]
    (log/info "Starting HTTP server on port" port)
    (reset! server
            (jetty/run-jetty app
                             {:port port
                              :join? false}))
    (log/info "HTTP server started")))

(defn stop!
  "Stop the HTTP server gracefully."
  []
  (when-let [s @server]
    (log/info "Stopping HTTP server...")
    (.stop s)
    (reset! server nil)
    (log/info "HTTP server stopped")))
