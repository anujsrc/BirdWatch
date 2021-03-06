(ns birdwatch.persistence.elastic
  (:gen-class)
  (:require
   [birdwatch.data :as d]
   [birdwatch.persistence.tools :as pt]
   [clojure.tools.logging :as log]
   [clojure.pprint :as pp]
   [clojurewerkz.elastisch.native           :as esn]
   [clojurewerkz.elastisch.native.document  :as esnd]
   [clojurewerkz.elastisch.native.response  :as esnrsp]
   [clojurewerkz.elastisch.rest             :as esr]
   [clojurewerkz.elastisch.rest.document    :as esd]
   [clojurewerkz.elastisch.query            :as q]
   [clojurewerkz.elastisch.rest.response    :as esrsp]
   [clojure.core.async :as async :refer [<! chan put! timeout go-loop]]))

(defn native-query [{:keys [query n from]} conf native-conn]
  "run a query on previous matching tweets"
  (let [search  (esnd/search native-conn (:es-index conf) "tweet" :query query :size n :from from :sort {:id :desc})
        hits (esnrsp/hits-from search)]
    (log/debug "Total hits:" (esnrsp/total-hits search) "Retrieved:" (count hits))
    (pt/get-source hits)))

(defn run-persistence-loop [persistence-chan conf conn]
  "run loop for persisting tweets"
  (go-loop [] (let [t (<! persistence-chan)]
                (try
                  (esd/put conn (:es-index conf) "tweet" (:id_str t) t)
                  (catch Exception ex (log/error ex "esd/put error"))))
           (recur)))

(defn run-rt-persistence-loop [rt-persistence-chan persistence-chan]
  "run loop for persisting retweets"
  (go-loop [] (when-let [rt (:retweeted_status (<! rt-persistence-chan))]
                (put! persistence-chan rt))
           (recur)))

(defn run-find-missing-loop [tweet-missing-chan missing-tweet-found-chan conf conn]
  "starts loop for finding missing tweets, puts result on missing-tweet-found-chan "
  (go-loop [] (let [req (<! tweet-missing-chan)
                    res (esd/get conn (:es-index conf) "tweet" (:id_str req))]
                (if res
                  (put! missing-tweet-found-chan
                        {:tweet (pt/strip-source res) :uid (:uid req)})
                  (log/debug "birdwatch.persistence missing" (:id_str req) res)))
           (recur)))

(defn run-query-loop [query-chan query-results-chan conf native-conn]
  "run loop for answering queries"
  (go-loop [] (let [q (<! query-chan) result (native-query q conf native-conn)]
                (log/debug "Received query:" q)
                (put! query-results-chan {:uid (:uid q) :result result}))
           (recur)))

(defn run-tweet-count-loop [tweet-count-chan conf conn]
  "run loop for sending stats about total number of indexed tweets"
  (go-loop [] (<! (timeout 10000))
           (let [cnt (:count (esd/count conn (:es-index conf) "tweet"))]
             (put! tweet-count-chan (format "%,15d" cnt)))
           (recur)))

