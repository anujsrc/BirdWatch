(ns birdwatch.persistence.component
  (:gen-class)
  (:require
   [birdwatch.data :as d]
   [birdwatch.persistence.tools :as pt]
   [birdwatch.persistence.elastic :as es]
   [clojure.tools.logging :as log]
   [clojure.pprint :as pp]
   [clojurewerkz.elastisch.native           :as esn]
   [clojurewerkz.elastisch.rest             :as esr]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as async :refer [<! chan go-loop tap]]))

(defrecord Persistence [conf channels conn native-conn]
  component/Lifecycle
  (start [component]
         (log/info "Starting Persistence Component")
         (let [conn (esr/connect (:es-address conf))
               native-conn (esn/connect [(:es-native-address conf)] {"cluster.name" (:es-cluster-name conf)})]
           (es/run-persistence-loop (:persistence channels) conf conn)
           (es/run-rt-persistence-loop (:rt-persistence channels) (:persistence channels))
           (es/run-find-missing-loop (:tweet-missing channels) (:missing-tweet-found channels) conf conn)
           (es/run-query-loop (:query channels) (:query-results channels) conf native-conn)
           (es/run-tweet-count-loop (:tweet-count channels) conf conn)
           (assoc component :conn conn :native-conn native-conn)))
  (stop [component] ;; TODO: proper teardown of resources
        (log/info "Stopping Persistence Component")
        (assoc component :conn nil :native-conn nil)))

(defn new-persistence [conf] (map->Persistence {:conf conf}))

(defrecord Persistence-Channels []
  component/Lifecycle
  (start [component] (log/info "Starting Persistence Channels Component")
         (assoc component
           :query (chan)
           :query-results (chan)
           :tweet-missing (chan)
           :missing-tweet-found (chan)
           :persistence (chan)
           :rt-persistence (chan)
           :tweet-count (chan)))
  (stop [component] (log/info "Stop Persistence Channels Component")
        (assoc component :query nil :query-results nil :tweet-missing nil :missing-tweet-found nil
                         :persistence nil :rt-persistence nil :tweet-count nil)))

(defn new-persistence-channels [] (map->Persistence-Channels {}))
